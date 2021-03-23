(ns ui.events.analysis
  (:require [goog.string :as gstr]))

(defn load-continuous-tree-analysis [_ [_ analysis-id]]
  {:dispatch [:graphql/query {:query (gstr/format
                                      "query {
                                         getContinuousTree(id: \"%s\") {
                                              maps 
                                              outputFileUrl
                                     }}" analysis-id)}]})
