(ns liskasys.cljs.person
  (:require [clj-brnolib.cljs.comp.buttons :as buttons]
            [clj-brnolib.cljs.comp.data-table :refer [data-table]]
            [clj-brnolib.cljs.comp.input-text :refer [input-text]]
            [clj-brnolib.cljs.util :as util]
            [clj-brnolib.validation :as validation]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [liskasys.cljc.util :as cljc-util]
            [liskasys.cljs.ajax :refer [server-call]]
            [liskasys.cljs.common :as common]
            [liskasys.cljs.pages :as pages]
            [re-com.core :as re-com]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [secretary.core :as secretary]
            [taoensso.timbre :as timbre]))

(defn page-persons []
  (let [persons (re-frame/subscribe [:entities :person])
        lunch-types (re-frame/subscribe [:entities :lunch-type])]
    (fn []
      [re-com/v-box
       :children
       [[:h3 "Osoby"]
        [re-com/hyperlink-href :label [re-com/button :label "Nový"] :href (str "#/person/e")]
        [data-table
         :table-id :persons
         :rows @persons
         :colls [["Příjmení" :person/lastname]
                 ["Jméno" :person/firstname]
                 #_["Variabilní symbol" :person/var-symbol]
                 #_["Dieta" #(:lunch-type/label (get @lunch-types (some-> % :person/lunch-type :db/id)))]
                 ["Rozvrh docházky" #(when (not= (:person/att-pattern %) "0000000") (:person/att-pattern %))]
                 ["Rozvrh obědů" #(when (not= (:person/lunch-pattern %) "0000000") (:person/lunch-pattern %))]
                 ["Email" :person/email]
                 ["Aktivní?" :person/active?]
                 ["Dítě?" :person/child?]
                 #_["Mobilní telefon" :person/phone]
                 [[re-com/md-icon-button
                   :md-icon-name "zmdi-refresh"
                   :tooltip "Přenačíst ze serveru"
                   :on-click #(re-frame/dispatch [:entities-load :person])]
                  (fn [row]
                    [re-com/h-box
                     :gap "5px"
                     :children
                     [[re-com/hyperlink-href
                       :href (str "#/person/" (:db/id row) "e")
                       :label [re-com/md-icon-button
                               :md-icon-name "zmdi-edit"
                               :tooltip "Editovat"]]
                      [buttons/delete-button #(re-frame/dispatch [:entity-delete :person (:db/id row)])]]])
                  :none]]]]])))

(defn page-person []
  (let [person (re-frame/subscribe [:entity-edit :person])
        lunch-types (re-frame/subscribe [:entities :lunch-type])
        persons (re-frame/subscribe [:entities :person])]
    (fn []
      (if-not (and @persons @lunch-types)
        [re-com/throbber]
        (let [item @person
              errors (:-errors item)]
          [re-com/v-box :gap "5px"
           :children
           [[:h3 "Osoba"]
            [re-com/label :label "Příjmení"]
            [input-text item :person :person/lastname]
            [re-com/label :label "Jméno"]
            [input-text item :person :person/firstname]
            [re-com/checkbox
             :label "aktivní člen?"
             :model (:person/active? item)
             :on-change #(re-frame/dispatch [:entity-change :person (:db/id item) :person/active? %])]
            [re-com/label :label "Variabilní symbol"]
            [input-text item :child :person/var-symbol util/parse-int]
            [re-com/label :label "Dieta"]
            [re-com/single-dropdown
             :model (some-> item :person/lunch-type :db/id)
             :on-change #(re-frame/dispatch [:entity-change :child (:db/id item) :person/lunch-type {:db/id %}])
             :choices (conj (util/sort-by-locale :label (vals @lunch-types)) {:db/id nil :label "běžná"})
             :id-fn :db/id
             :label-fn :lunch-type/label
             :placeholder "běžná"
             :width "250px"]
            [re-com/label :label "Rozvrh obědů"]
            [re-com/h-box :gap "5px"
             :children
             [[input-text item :person :person/lunch-pattern]
              "poútstčtpásone: 0 = bez oběda, 1-9 = požadovaný počet obědů"]]
            [re-com/checkbox
             :label "obědy zdarma?"
             :model (:person/free-lunches? item)
             :on-change #(re-frame/dispatch [:entity-change :person (:db/id item) :person/free-lunches? %])]
            [re-com/checkbox
             :label "dítě?"
             :model (:person/child? item)
             :on-change #(re-frame/dispatch [:entity-change :person (:db/id item) :person/child? %])]
            (if (:person/child? item)
              [re-com/v-box
               :children
               [[re-com/label :label "Rozvrh docházky"]
                [re-com/h-box :gap "5px"
                 :children
                 [[input-text item :person :person/att-pattern]
                  "poútstčtpásone: 0 = bez docházky, 1 = celodenní, 2 = půldenní"]]
                [re-com/checkbox
                 :label "docházka zdarma?"
                 :model (:person/free-att? item)
                 :on-change #(re-frame/dispatch [:entity-change :person (:db/id item) :person/free-att? %])]
                (when (:db/id item)
                  [re-com/v-box
                   :children
                   [[:h3 "Rodiče"]
                    [:table
                     [:tbody
                      [:tr
                       (doall
                        (for [parent (->> (:person/parent item)
                                          (map (comp @persons :db/id))
                                          (util/sort-by-locale cljc-util/person-fullname))]
                          ^{:key (:db/id parent)}
                          [:td
                           [re-com/label :label (cljc-util/person-fullname parent)]
                           [buttons/delete-button
                            #(re-frame/dispatch [:common/retract-ref-many :person {:db/id (:db/id item)
                                                                                   :person/parent (:db/id parent)}])
                            :below-center]]))]
                      [:tr
                       [:td
                        [re-com/single-dropdown
                         :model nil
                         :on-change #(re-frame/dispatch [:entity-change :person (:db/id item) :person/parent (fn [v] (conj v {:db/id %}))])
                         :choices (->> (apply dissoc @persons (map :db/id (:person/parent item)))
                                       vals
                                       (remove :person/child?)
                                       (util/sort-by-locale cljc-util/person-fullname))
                         :id-fn :db/id
                         :label-fn cljc-util/person-fullname
                         :placeholder "Přidat rodiče..."
                         :filter-box? true
                         :width "250px"]]]]]]])]]
              [re-com/v-box
               :children
               [[re-com/label :label "Email"]
                [input-text item :person :person/email]
                [re-com/label :label "Telefon"]
                [input-text item :person :person/phone]
                [re-com/label :label "Role"]
                [re-com/h-box :align :center :gap "5px"
                 :children
                 [[input-text item :person :person/roles]
                  "admin, obedy"]]]])
            [re-com/h-box :align :center :gap "5px"
             :children
             [[re-com/button :label "Uložit" :class "btn-success" :on-click #(re-frame/dispatch [:entity-save :person])]
              "nebo"
              [re-com/hyperlink-href :label [re-com/button :label "Nový"] :href (str "#/person/e")]
              [re-com/hyperlink-href :label [re-com/button :label "Seznam"] :href (str "#/persons")]]]]])))))

(secretary/defroute "/persons" []
  (re-frame/dispatch [:set-current-page :persons]))
(pages/add-page :persons #'page-persons)

(secretary/defroute #"/person/(\d*)(e?)" [id edit?]
  (re-frame/dispatch [:entity-set-edit :person (util/parse-int id) (not-empty edit?)])
  (re-frame/dispatch [:set-current-page :person]))
(pages/add-page :person #'page-person)
(common/add-kw-url :person "person")


