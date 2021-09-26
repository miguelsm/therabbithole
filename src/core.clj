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
   :summary-content [:#page :> :#main :> :#primary :> :#content :> :article :> :.entry-content :> (html/but :.sharedaddy)]
   :section-heading [(html/re-pred #"((?i)summary|key takeaways|what i got out of it)(:)*")]})

(defn summary-urls [url]
  (let [dom   (html/html-resource (java.net.URL. url))
        links (html/select dom (:library-link selectors))
        urls  (mapv #(-> % (html/attr-values :href) first) links)]
    urls))

(def allowed-class-token (str (java.util.UUID/randomUUID)))

(defn enlive->hiccup [el]
  (if (string? el)
    (when-let [s (seq (str/replace el #"\n|\t" ""))]
      (str/join "" s))
    (when (:tag el)
      (->> (map enlive->hiccup (:content el))
           (concat [(:tag el)
                    (into {}
                      (filter (fn [[k v]]
                                (or (contains? #{:href :src} k)
                                    (and (= k :class)
                                         (str/includes? v allowed-class-token))))
                              (:attrs el)))])
           vec))))

(defn url->file-name [url]
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
                         (assoc-in x [1 :style] {:float "right"})
                         (update-in x [1 :src] #(str "http://localhost:8080/300,fit/" %)))
        header-h1  (->> (html/select header [:h1])
                        (map enlive->hiccup)
                        first)
        header-id  (-> header-h1 last csk/->kebab-case)]
    (concat
      [(into [:h2 {:id header-id}] (last header-h1))
       [:div {:class "book-cover"}
        header-img
        [:br]
        [:a {:href buy-link} "Buy this book"]]]
      (->> (html/at
             (html/select nodes (:summary-content selectors))
             (:section-heading selectors)
             (html/wrap "span" {:class (str/join " " ["section-heading" allowed-class-token])}))
           (mapv enlive->hiccup)))))

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

(defn download-html
  "Save the html from the web into a local file."
  ([url]
   (download-html url (str "in/html/" (url->file-name url) ".html")))
  ([url file-name]
   (when-not (.exists (io/file file-name))
     (sspit file-name (slurp url)))))

(defn process-url [url]
  (let [file-name        (url->file-name url)
        input-html-file  (str "in/html/" file-name ".html")
        output-html-file (str "out/html/" file-name ".html")
        toc-html-file    (str "out/toc/" file-name ".html")
        #_#_#_#_
        md               (html->md html)
        _                (sspit (str "out/md/" file-name ".md") md)]
    (when-not (.exists (io/file output-html-file))
      (sspit output-html-file (-> input-html-file
                                  extract-summary
                                  hiccup/html
                                  (str "\n"))))
    (when-not (.exists (io/file toc-html-file))
      (sspit toc-html-file (-> input-html-file
                               extract-toc
                               hiccup/html
                               (str "\n"))))))

(comment

  ;; save the list of summary urls into a "urls" file
  (spit "urls" (summary-urls library-url))

  (def urls (read-string (slurp "urls")))

  (count urls)

  (doseq [url urls]
    (try
      (log/info "Downloading" url)
      (download-html url)
      (catch Exception e
        (log/error "Error downloading" url ": " (.getMessage e)))))

  (doseq [url urls]
    (try
      (log/info "Processing" url)
      (process-url url)
      (catch Exception e
        (log/error "Error processing" url ": " (.getMessage e)))))

  ,)
