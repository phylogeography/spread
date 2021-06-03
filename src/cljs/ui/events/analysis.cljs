(ns ui.events.analysis
  #_(:require [goog.string :as gstr]))

#_(defn load-continuous-tree-analysis [_ [_ analysis-id]]
  {:dispatch [:graphql/query {:query (gstr/format
                                      "query {
                                         getContinuousTree(id: \"%s\") {
                                              maps
                                              outputFileUrl
                                     }}" analysis-id)}]})
