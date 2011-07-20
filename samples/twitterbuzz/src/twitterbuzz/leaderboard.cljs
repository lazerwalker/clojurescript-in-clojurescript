;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns twitterbuzz.leaderboard
  (:require [twitterbuzz.core :as buzz]
            [goog.dom :as dom]))

(defn add-leaderboard-node [node]
  (let [user (first node)
        user-info (second node)
        parent (dom/getElement "leaderboard-content")
        child (buzz/dom-element :div {:class "tweet"})
        details (buzz/dom-element :div {:class "tweet-details"})
        user-e (buzz/dom-element :div {:class "user-name"})
        text (buzz/dom-element :div {:class "tweet-text"})
        pic (buzz/dom-element :img {:src (:image-url user-info) :class "profile-pic"})
        num-mentions (buzz/dom-element "div")]
    (do (dom/insertChildAt text (dom/htmlToDocumentFragment (:last-tweet user-info)) 0) ;; (dom/setTextContent text (:last-tweet user-info))
        (dom/setTextContent user-e user)
        (dom/setTextContent num-mentions (str (buzz/num-mentions user-info)))
        (dom/appendChild child pic)
	(dom/appendChild child details)
        (dom/appendChild details user-e)
        (dom/appendChild details text)
        (dom/appendChild details num-mentions)
        (dom/appendChild parent child 0))))

(defn leaders [nodes]
  (reverse (sort-by #(buzz/num-mentions (second %)) nodes)))

(defn redraw-leaderboard [top-leaders]
  (do (buzz/remove-children "leaderboard-content")
      (doseq [next-node (seq top-leaders)]
        (add-leaderboard-node next-node))))

(defn- has-user-info? [graph-entry]
  (if (:last-tweet graph-entry) true false))

(defn retrieve-missing-info [users callback]
  (let [q (apply str (interpose " OR " (map (fn [[username user-info]] (str "from:" username)) users)))]
    (buzz/retrieve (.strobj {"q" q "rpp" 100}) callback)))

(defn receive-user-info [json have-info missing-info]
  (let [result-map (:results (js->clj json :keywordize-keys true))
	new-tweets (reduce (fn [m tweet]
				       (if (get m (:from_user tweet))
					 m
					 (assoc m (:from_user tweet) tweet)))
				     {} result-map)]
    (redraw-leaderboard (into have-info new-tweets))))

(defn update-leaderboard [graph]
  (let [top5 (take 5 (leaders (seq graph)))
	have-info (filter has-user-info? top5)
	missing-info (filter (complement has-user-info?) top5)]
    (if (empty? missing-info)
      (redraw-leaderboard top5)
      (retrieve-missing-info missing-info (fn [json]
					    (receive-user-info json have-info missing-info))))))

(buzz/register :track-clicked #(buzz/remove-children "leaderboard-content"))
(buzz/register :graph-update update-leaderboard)
