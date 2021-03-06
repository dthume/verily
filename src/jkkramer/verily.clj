(ns jkkramer.verily
  (:require [clojure.string :as string]))

(defn seqify [x]
  (if-not (sequential? x) [x] x))

(defn make-validator [keys bad-pred msg]
  (let [bad-pred* #(try
                     (bad-pred %)
                     (catch Exception _ true))]
    (fn [m]
      (let [bad-keys (filter #(bad-pred* (get m % ::absent))
                             (seqify keys))]
        (when (seq bad-keys)
          (if (map? msg)
            msg
            {:keys bad-keys :msg msg}))))))

(defn contains
  "The keys must be present in the map but may be blank."
  [keys & [msg]]
  (make-validator keys #{::absent}
                  (or msg "must be present")))

(defn required
  "The keys must be present in the map AND not be blank."
  [keys & [msg]]
  (make-validator keys #(or (= ::absent %)
                            (nil? %)
                            (and (string? %) (string/blank? %)))
                  (or msg "must not be blank")))

(defn not-blank
  "If present, the keys must not be blank."
  [keys & [msg]]
  (make-validator keys #(or (nil? %)
                            (and (string? %) (string/blank? %)))
                  (or msg "must not be blank")))

(defn exact [val keys & [msg]]
  (make-validator keys #(and (not= ::absent %) (not= val %))
                  (or msg "incorrect value")))

(defn equal [keys & [msg]]
  (let [keys (seqify keys)]
    (fn [m]
      (when-not (apply = (map #(get m %) keys))
        (if (map? msg)
          msg
          {:keys keys :msg (or msg "must be equal")})))))

(defn matches [re keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (string/blank? %))
               (not (re-matches re %)))
    (or msg "incorrect format")))

(defn min-length [len keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (not (<= len (count %))))
    (or msg (str "must be at least " len " characters"))))

(defn max-length [len keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (not (>= len (count %))))
    (or msg (str "cannot exceed " len " characters"))))

(defn in [coll keys & [msg]]
  (let [coll-set (if (set? coll)
                   coll (set coll))]
    (make-validator
      keys #(and (not= ::absent %)
                 (not (nil? %))
                 (not (contains? coll-set %)))
      (or msg (str "not an accepted value")))))

(def ^:private zip-regex #"^\d{5}(?:[-\s]\d{4})?$")

(defn us-zip [keys & [msg]]
  (make-validator keys #(and (not= ::absent %)
                             (not (string/blank? %))
                             (not (re-matches zip-regex %)))
                  (or msg "must be a valid US zip code")))

(defn email [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (string/blank? %))
               (not (re-matches #"[^^]+@[^$]+" %))) ;RFC be damned
    (or msg "must be a valid email")))

(defn web-url [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (string/blank? %))
               (or (not (try
                          (java.net.URL. %)
                          (catch Exception _)))
                   (not (re-find #"^https?://" %))))
    (or msg "must be a valid website URL")))

(defn url [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (string/blank? %))
               (not (try
                      (java.net.URL. %)
                      (catch Exception _))))
    (or msg "must be a valid URL")))

(defn string [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %)) (not (string? %)))
    (or msg "must be a string")))

(defn strings [keys & [msg]]
  (make-validator
    keys (fn [v]
           (and (not= ::absent v)
                (or (and (not (nil? v)) (not (sequential? v)))
                    (some #(not (string? %)) v))))
    (or msg "must be strings")))

(defn bool [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %)) (not (true? %)) (not (false? %)))
    (or msg "must be true or false")))

(defn bools [keys & [msg]]
  (make-validator
    keys (fn [v]
           (and (not= ::absent v)
                (or (and (not (nil? v)) (not (sequential? v)))
                    (some #(and (not (true? %)) (not (false? %))) v))))
    (or msg "must be all true or false")))

(defn integer [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %)) (not (integer? %)))
    (or msg "must be a number")))

(defn integers [keys & [msg]]
  (make-validator
    keys (fn [v]
           (and (not= ::absent v)
                (or (and (not (nil? v)) (not (sequential? v)))
                    (some #(not (integer? %)) v))))
    (or msg "must be numbers")))

(defn floating-point [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %)) (not (float? %)))
    (or msg "must be a decimal number")))

(defn floating-points [keys & [msg]]
  (make-validator
    keys (fn [v]
           (and (not= ::absent v)
                (or (and (not (nil? v)) (not (sequential? v)))
                    (some #(not (float? %)) v))))
    (or msg "must be decimal numbers")))

(defn decimal [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %)) (not (decimal? %)))
    (or msg "must be a decimal number")))

(defn decimals [keys & [msg]]
  (make-validator
    keys (fn [v]
           (and (not= ::absent v)
                (or (and (not (nil? v)) (not (sequential? v)))
                    (some #(not (decimal? %)) v))))
    (or msg "must be decimal numbers")))

(defn min-val [min keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or (not (number? %)) (> min %)))
    (or msg (str "cannot be less than " min))))

(def at-least min-val)

(defn max-val [max keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or (not (number? %)) (< max %)))
    (or msg (str "cannot be more than " max))))

(def at-most max-val)

(defn within [min max keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or (not (number? %)) (or (> min %) (< max %))))
    (or msg (str "must be within " min " and " max))))

(defn positive [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or (not (number? %)) (not (pos? %))))
    (or msg "must be a positive number")))

(defn negative [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or (not (number? %)) (not (neg? %))))
    (or msg "must be a negative number")))

(defn date [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %))
               (not (instance? java.util.Date %)))
    (or msg "must be a date")))

(defn dates [keys & [msg]]
  (make-validator
    keys (fn [v]
           (and (not= ::absent v)
                (or (and (not (nil? v)) (not (sequential? v)))
                    (some #(not (instance? java.util.Date %)) v))))
    (or msg "must be dates")))

(defn after [date keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or
                 (not (instance? java.util.Date date))
                 (not (.after ^java.util.Date % date))))
    (or msg (str "must be after " date))))

(defn before [date keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %)
               (not (nil? %))
               (or
                 (not (instance? java.util.Date date))
                 (not (.before ^java.util.Date % date))))
    (or msg (str "must be before " date))))

(defn- digits [n]
  (map #(Character/digit % 10) (str n)))
 
(defn- luhn? [x]
  (let [n (if (string? x)
            (string/replace x #"[^0-9]" "")
            x)
        sum (reduce + (map
                        (fn [d idx]
                          (if (even? idx)
                            (reduce + (digits (* d 2)))
                           d))
                        (reverse (digits n))
                        (iterate inc 1)))]
    (zero? (mod sum 10))))

(defn luhn [keys & [msg]]
  (make-validator
    keys #(and (not= ::absent %) (not (nil? %)) (not (luhn? %)))
    (or msg "number is not valid")))

(defn combine [& validators]
  (fn [m]
    (apply concat (map seqify (keep #(% m) validators)))))

(def validations-map
  {:contains contains
   :required required
   :not-blank not-blank
   :exact exact
   :equal equal
   :matches matches
   :min-length min-length
   :max-length max-length
   :in in
   :us-zip us-zip
   :email email
   :url url
   :web-url web-url
   :str string
   :string string
   :strs strings
   :strings strings
   :bool bool
   :boolean bool
   :bools bools
   :booleans bools
   :integer integer
   :integers integers
   :floating-point floating-point
   :floating-points floating-points
   :float floating-point
   :floats floating-points
   :decimal decimal
   :decimals decimals
   :min-val min-val
   :at-least at-least
   :max-val max-val
   :at-most at-most
   :within within
   :positive positive
   :negative negative
   :date date
   :after after
   :before before
   :luhn luhn})

(defmulti validation->fn (fn [vspec] (first vspec)))

(defmethod validation->fn :default [vspec]
  (if-let [vfn (get validations-map (first vspec))]
    (apply vfn (rest vspec))
    (throw (IllegalArgumentException.
             (str "Unknown validation " (first vspec))))))

(defn validations->fn [validations]
  (apply combine (map validation->fn validations)))

(defn validate [values validations]
  ((validations->fn validations) values))