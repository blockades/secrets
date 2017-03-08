;; FXC - PIN Secret Sharingdigital  social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015-2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns fxc.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]

            [hiccup.page :as page]
            [fxc.form_helpers :as fh]
            [fxc.webpage :as web]
            [formidable.parse :as fp]
            [formidable.core :as fc]

            [fxc.secretshare :as ssss]
            [fxc.random :as rand]
            [fxc.fxc :as fxc]
            [json-html.core :as present]
            [hiccup.page :as page]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

;; defaults
(def settings
  {:total (Integer. 5)
   :quorum (Integer. 3)

   :prime 'prime4096

   :description "FXC v1 (Freecoin Secret Sharing)"

   ;; versioning every secret
   :protocol "FXC1"

   :type "WEB"

   ;; random number generator settings
   :length 6
   :entropy 3.1})


(def generate-form-spec
  {:renderer :bootstrap3-stacked
   :fields [{:name "PIN:"   :type :text}]
   :validations [[:required [:field :value]]]
   :action "/generate"
   :method "post"})

(def recovery-form-spec
  {:renderer :bootstrap3-stacked
   :fields [{:name "1"  :type :integer}
            {:name "2"  :type :text}
            {:name "3"  :type :text}]
   :validations [[:required ["1" "2" "3"]]]
                 
   :action "/recover"
   :method "post"})

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

(defn add-pos-to-share
  "add a final cipher to each number in the shares collection which
  indicates its position, returns an array of strings"
  [shares]
    (loop [res []
           s (first shares)
           c 1]
      (if (< c (count shares))
        (recur (conj res (format "%d%d" s c))
               (nth shares c)
               (inc c))
        (conj res (format "%d%d" s c)))))
  
           
    
          


(defroutes app-routes

  (GET "/" []
      (web/render-page
       {:title "PIN Secret Sharing"
        :body [:div {:class "secrets row center"}
               [:div {:class "content input-form"}
                (fc/render-form generate-form-spec)]]}))

  (POST "/g*" {params :params}
    (let [pin    (biginteger (params "PIN:"))
          shares (:shares (ssss/shamir-split settings pin))]

      (web/render-page
       {:title "PIN Secret Sharing"
        :body [:div {:class "secrets row center"}
               [:div {:class "password"}
                "Your PIN:" [:div {:class "content"} pin]]

               [:div {:class "slices"}
                [:h3 (str "Split in " (:total settings)
                          " shared secrets, quorum "
                          (:quorum settings) ":")]
                [:ul (map #(conj [:li {:class "content"}] %) 
                          (add-pos-to-share shares))]]]})))


  (GET "/r*" []
    (web/render-page {:title "PIN Secret Recovery"
                  :body [:div {:class "recovery row center"}
                         [:div {:class "content input-form"}
                          (fc/render-form recovery-form-spec)
                          ]]}))

  (POST "/r*" {params :params}
    (web/render-page {:title "PIN Secret  Recovery"
                  :body (let [para (fp/parse-params recovery-form-spec params)
                              ordered (sort-by last (vals para))
                              trimmed (map #(subs % 0 (dec (count %))) ordered)
                              converted (map biginteger trimmed)]
                          (present/edn->html
                           {:0 (count converted)
                            :header settings
                            :shares converted
                            :result (ssss/shamir-combine {:header settings
                                                          :shares converted})
                                               }))}))
  ;; TODO: detect cryptographical conversion error: returned is the first share

  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
