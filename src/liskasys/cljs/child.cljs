(ns liskasys.cljs.child
  (:require [clj-brnolib.cljs.comp.buttons :as buttons]
            [clj-brnolib.cljs.comp.data-table :refer [data-table]]
            [clj-brnolib.cljs.util :as util]
            [clj-brnolib.validation :as validation]
            [cljs.pprint :refer [pprint]]
            [liskasys.cljs.ajax :refer [server-call]]
            [liskasys.cljs.common :as common]
            [liskasys.cljs.pages :as pages]
            [re-com.core :as re-com]
            [re-frame.core :as re-frame]
            [secretary.core :as secretary]
            [taoensso.timbre :as timbre]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [clj-brnolib.cljs.comp.input-text :refer [input-text]]))

(re-frame/register-handler
 ::save
 common/debug-mw
 (fn [db [_ reply]]
   (timbre/debug "test reply" reply)
   db))

(re-frame/register-handler
 ::saved
 common/debug-mw
 (fn [db [_ reply]]
   (timbre/debug "saved" reply)
   db))

(defn page-children []
  (let [children (re-frame/subscribe [:entities :child])]
    (fn []
      [re-com/v-box
       :children
       [[:h3 "Děti"]
        [re-com/hyperlink-href :label [re-com/button :label "Nové"] :href (str "#/child/e")]
        [data-table
         :table-id :children
         :rows @children
         :colls [["Jméno" :firstname]
                 ["Příjmení" :lastname]
                 ["Variabilní symbol" :var-symbol]
                 [[re-com/md-icon-button
                   :md-icon-name "zmdi-refresh"
                   :tooltip "Přenačíst ze serveru"
                   :on-click #(re-frame/dispatch [:entities-load :child])]
                  (fn [row]
                    [re-com/h-box
                     :gap "5px"
                     :children
                     [[re-com/hyperlink-href
                       :href (str "#/child/" (:id row) "e")
                       :label [re-com/md-icon-button
                               :md-icon-name "zmdi-edit"
                               :tooltip "Editovat"]]
                      [buttons/delete-button #(re-frame/dispatch [:entity-delete :child (:id row)])]]])
                  :none]]]]])))

(defn page-child []
  (let [child (re-frame/subscribe [:entity-edit :child])
        user-childs (re-frame/subscribe [:entities :user-child])
        users (re-frame/subscribe [:entities :user])
        validation-fn #(cond-> {}
                         (str/blank? (:firstname %))
                         (assoc :firstname "Vyplňte správně jméno")
                         (str/blank? (:lastname %))
                         (assoc :lastname "Vyplňte správně příjmení")
                         (not (:var-symbol %))
                         (assoc :var-symbol "Vyplňte správně celočíselný variabilní symbol")
                         true
                         timbre/spy)]
    (fn []
      (if-not (and @users @user-childs)
        [re-com/throbber]
        (let [item @child
              errors (:-errors item)
              child-users (->> @user-childs
                               vals
                               (keep #(when (= (:id item) (:child-id %))
                                        (assoc % :-fullname (:-fullname (get @users (:user-id %))))))
                               (util/sort-by-locale :-fullname))
              sorted-users (->> (apply dissoc @users (map :user-id child-users))
                                vals
                                (util/sort-by-locale :-fullname))]
          [re-com/v-box :gap "5px"
           :children
           [[:h3 "Dítě"]
            [:table
             [:tbody
              [:tr
               [:td [re-com/label :label "Jméno"]]
               [:td [re-com/label :label "Příjmení"]]
               [:td [re-com/label :label "Variabilní symbol"]]]
              [:tr
               [:td [input-text item :child :firstname]]
               [:td [input-text item :child :lastname]]
               [:td [input-text item :child :var-symbol util/parse-int]]]]]
            [re-com/button :label "Uložit" :class "btn-success" :on-click #(re-frame/dispatch [:entity-save :child validation-fn])]
            [:h3 "Rodiče"]
            (when (:id item)
              [:table
               [:tbody
                [:tr
                 (doall
                  (for [child-user child-users]
                    ^{:key (:id child-user)}
                    [:td
                     [re-com/label :label (:-fullname child-user)]
                     [buttons/delete-button #(re-frame/dispatch [:entity-delete :user-child (:id child-user)])]]))]
                [:tr
                 [:td
                  [re-com/single-dropdown
                   :model nil
                   :choices sorted-users
                   :label-fn :-fullname
                   :placeholder "Přidat rodiče..."
                   :on-change #(server-call [:user-child/save {:user-id % :child-id (:id item)}]
                                            nil
                                            [:entity-saved :user-child])
                   :width "250px"]]]]])
            [:pre (with-out-str (pprint item))]]])))))

(secretary/defroute "/children" []
  (re-frame/dispatch [:set-current-page :children]))
(pages/add-page :children #'page-children)

(secretary/defroute #"/child/(\d*)(e?)" [id edit?]
  (re-frame/dispatch [:entity-set-edit :child (util/parse-int id) (not-empty edit?)])
  (re-frame/dispatch [:set-current-page :child]))
(pages/add-page :child #'page-child)
(common/add-kw-url :child "child")

