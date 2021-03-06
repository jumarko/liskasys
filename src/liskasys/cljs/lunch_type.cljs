(ns liskasys.cljs.lunch-type
  (:require [clojure.string :as str]
            [liskasys.cljc.util :as cljc.util]
            [liskasys.cljs.common :as common]
            [liskasys.cljs.comp.buttons :as buttons]
            [liskasys.cljs.comp.data-table :refer [data-table]]
            [liskasys.cljs.comp.history :as history]
            [liskasys.cljs.comp.input-text :refer [input-text]]
            [liskasys.cljs.pages :as pages]
            [re-com.core :as re-com]
            [re-frame.core :as re-frame]
            [secretary.core :as secretary]
            [taoensso.timbre :as timbre]))

(defn page-lunch-types []
  (let [lunch-types (re-frame/subscribe [:entities :lunch-type])
        table-state (re-frame/subscribe [:table-state :lunch-types])]
    (fn []
      [re-com/v-box
       :children
       [[:h3 "Diety"]
        [data-table
         :table-id :lunch-types
         :rows lunch-types
         :colls [[[re-com/h-box :gap "5px" :justify :end
                   :children
                   [[re-com/md-icon-button
                     :md-icon-name "zmdi-plus-square"
                     :tooltip "Přidat"
                     :on-click #(set! js/window.location.hash "#/lunch-type/e")]
                    [re-com/md-icon-button
                     :md-icon-name "zmdi-refresh"
                     :tooltip "Přenačíst ze serveru"
                   :on-click #(re-frame/dispatch [:entities-load :lunch-type])]]]
                  (fn [row]
                    (when (= (:db/id row) (:selected-row-id @table-state))
                      [re-com/h-box :gap "5px" :justify :end
                       :children
                       [[re-com/hyperlink-href
                         :href (str "#/lunch-type/" (:db/id row) "e")
                         :label [re-com/md-icon-button
                                 :md-icon-name "zmdi-edit"
                                 :tooltip "Editovat"]]
                        [buttons/delete-button :on-confirm #(re-frame/dispatch [:entity-delete :lunch-type (:db/id row)])]]]))
                  :none]
                 ["Název" :lunch-type/label]
                 #_["Barva" :lunch-type/color]]]]])))

(defn page-lunch-type []
  (let [lunch-type (re-frame/subscribe [:entity-edit :lunch-type])
        validation-fn #(cond-> {}
                         (str/blank? (:lunch-type/label %))
                         (assoc :lunch-type/label "Vyplňte název")
                         ;;(str/blank? (:lunch-type/color %))
                         ;;(assoc :lunch-type/color "Vyplňte barvu")
                         )]
    (fn []
      (let [item @lunch-type
            errors (:-errors item)]
        [re-com/v-box :gap "5px"
         :children
         [[:h3 "Dieta"]
          [re-com/label :label "Název"]
          [input-text item :lunch-type :lunch-type/label]
          #_[re-com/label :label "Barva"]
          #_[input-text item :lunch-type :lunch-type/color]
          [re-com/h-box :align :center :gap "5px"
           :children
           [[re-com/button :label "Uložit" :class "btn-success" :on-click #(re-frame/dispatch [:entity-save :lunch-type validation-fn])]
            "nebo"
            (when (:db/id item)
              [re-com/hyperlink-href :label [re-com/button :label "Nová"] :href (str "#/lunch-type/e")])
            [re-com/hyperlink-href :label [re-com/button :label "Seznam"] :href (str "#/lunch-types")]]]
          [history/view (:db/id item)]]]))))

(secretary/defroute "/lunch-types" []
  (re-frame/dispatch [:set-current-page :lunch-types]))
(pages/add-page :lunch-types #'page-lunch-types)

(secretary/defroute #"/lunch-type/(\d*)(e?)" [id edit?]
  (re-frame/dispatch [:entity-set-edit :lunch-type (cljc.util/parse-int id) (not-empty edit?)])
  (re-frame/dispatch [:set-current-page :lunch-type]))
(pages/add-page :lunch-type #'page-lunch-type)
(common/add-kw-url :lunch-type "lunch-type")
