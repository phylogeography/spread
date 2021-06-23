(ns ui.component.progress)

#_(defn progress-bar [{:keys [label progress class max]
                     :or   {progress 0
                            max      1}}]
  [:progress {:class class :max max :value progress :label label}])
