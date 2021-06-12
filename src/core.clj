(ns core
  (:require [camel-snake-kebab.core :as csk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup2.core :as hiccup]
            [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log])
  (:import
   (io.github.furstenheim CopyDown)))

(def library-url "https://blas.com/library/")

(def selectors
  {:library-link    [:#page :> :#main :> :#primary :> :#content :> :article :> :div :> [:p (html/nth-child 3)] :> :a]
   :summary-header  [:#page :> :#main :> :#primary :> :#content :> :article :> :header]
   :summary-content [:#page :> :#main :> :#primary :> :#content :> :article :> :.entry-content :> (html/but :.sharedaddy)]})

(defn summary-urls [url]
  (let [dom   (html/html-resource (java.net.URL. url))
        links (html/select dom (:library-link selectors))
        urls  (mapv #(-> % (html/attr-values :href) first) links)]
    urls))

(defn enlive->hiccup [el]
  (if-not (string? el)
    (->> (map enlive->hiccup (:content el))
         (concat [(:tag el) (select-keys (:attrs el) [:href :src])])
         (keep identity)
         vec)
    (when-let [s (seq (str/replace el #"\n|\t" ""))]
      (str/join "" s))))

(defn url->filename [url]
  (-> url
      (str/split #"\/")
      last))

(defn extract-summary
  "Extracts the summary from a page and returns it in hiccup format."
  [file]
  (let [nodes      (html/html-resource (java.io.StringReader. (slurp file)))
        header     (html/select nodes (:summary-header selectors))
        buy-link   (-> header (html/select [:a]) first :attrs :href)
        header-img (as-> (html/select header [:img]) x
                         (map enlive->hiccup x)
                         (first x)
                         (assoc-in x [1 :style] {:float "right"}))
        header-h1  (->> (html/select header [:h1])
                        (map enlive->hiccup)
                        first)
        header-id  (-> header-h1 last csk/->kebab-case)]
    #_(into [])
    (concat
      [(into [:h2 {:id header-id}] (last header-h1))
       [:div {:class "book-cover"}
        header-img
        [:br]
        [:a {:href buy-link} "Buy this book"]]]
      (mapv enlive->hiccup (html/select nodes (:summary-content selectors))))))

(defn extract-toc
  [file]
  (let [nodes     (html/html-resource (java.io.StringReader. (slurp file)))
        header    (html/select nodes (:summary-header selectors))
        header-h1 (->> (html/select header [:h1])
                       (map enlive->hiccup)
                       first)]
    [:li [:a {:href (str "#" (-> header-h1 last csk/->kebab-case))}]]))

(defn html->md [html]
  (str "\\newpage\n\n" (.convert (CopyDown.) html) "\n\n\\pagebreak\n"))

(defn sspit
  "Like spit but creates parent directories."
  [f content]
  (io/make-parents f)
  (spit f content))

(defn process-url [url]
  (let [file-name       (url->filename url)
        input-html-file (str "in/html/" file-name ".html")]
    ;; save the html from the web into a local file
    (when-not (.exists (io/file input-html-file))
      (sspit input-html-file (slurp url)))
    (let [html (-> input-html-file
                   extract-summary
                   hiccup/html
                   (str "\n"))
          _    (sspit (str "out/html/" file-name ".html") html)
          toc  (-> input-html-file
                   extract-toc
                   hiccup/html
                   (str "\n"))
          _    (sspit (str "out/toc/" file-name ".html") toc)
          #_#_#_#_
          md   (html->md html)
          _    (sspit (str "out/md/" file-name ".md") md)])))

(comment

  (def urls (read-string (slurp "urls")))

  (process-url (first urls))

  #_header

  ;; save the list of summary urls into a "urls" file
  (spit "urls" (summary-urls library-url))

  (doseq [url (take 25 urls)]
    (try
      (log/info "Processing" url)
      (process-url url)
      (catch Exception e
        (log/error "Error processing" url ": " (.getMessage e)))))

  ,)
