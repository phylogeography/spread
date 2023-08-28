(ns analysis-viewer.events.filters)

(defn add-attribute-filter [db [_ attr-id]]
  (let [max-id (or (->> (:analysis.data/filters db)
                        keys
                        (apply max))
                   0)
        new-filter-id (inc max-id)
        attr (get (:analysis/attributes db) attr-id)
        new-filter-gen {:filter/id new-filter-id
                        :attribute/id attr-id}
        new-filter-spec (case (:attribute/type attr)
                          ; filter start with the full range
                          :linear {:filter/type :linear-filter
                                   :full-range (:range attr)
                                   :range      (:range attr)}
                          ; filter start with empty domain checked
                          :ordinal {:filter/type :ordinal-filter
                                    :filter-set #{}})]
    (assoc-in db [:analysis.data/filters new-filter-id] (merge new-filter-gen
                                                               new-filter-spec))))

(defn rm-attribute-filter [db [_ filter-id]]
  (update db :analysis.data/filters dissoc filter-id))

(defn set-linear-attribute-filter-range [db [_ filter-id range]]
  (assoc-in db [:analysis.data/filters filter-id :range] range))

(defn add-ordinal-attribute-filter-item [db [_ filter-id item]]
  (update-in db [:analysis.data/filters filter-id :filter-set] conj item))

(defn rm-ordinal-attribute-filter-item [db [_ filter-id item]]
  (update-in db [:analysis.data/filters filter-id :filter-set] disj item))
