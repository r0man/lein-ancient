(ns 
  leiningen.ancient.utils.io
  (:require [clojure.java.io :as io :only [file writer]]
            [leiningen.core.project :as prj]
            [ancient-clj.verbose :refer :all])
  (:import java.io.File))

;; ## Console Interaction

(defn prompt
  "Create a yes/no prompt using the given message."
  [& msg]
  (let [msg (str (apply str msg) " [yes/no] ")]
    (loop [i 3]
      (when (pos? i)
        (print msg)
        (.flush ^java.io.Writer *out*)
        (let [r (or (read-line) "")
              r (.toLowerCase ^String r)]
          (cond (= r "yes") true
                (= r "no") false
                :else (recur (dec i))))))))

;; ## Backup/Data Files

(defn create-backup-file!
  "Create backup of a given File. Print errors and return `nil` if a failure occurs."
  ^File
  [^File f settings]
  (let [^File parent (.getParentFile f)
        ^File backup (io/file parent (str (.getName f) ".backup"))]
    (try
      (when (or (:overwrite-backup settings)
                (not (.exists backup))
                (prompt "Do you want to overwrite the existing backup file?")) 
        (verbose "Creating backup at: " (.getCanonicalPath backup))
        (io/copy f backup) 
        backup)
      (catch Exception ex
        (println (red "Could not create backup file:") (.getMessage ex))
        nil))))

(defn delete-backup-file!
  [^File backup]
  (try
    (verbose "Deleting backup file ...")
    (.delete backup)
    (catch Exception ex 
      (println (red "Could not delete backup file " (.getPath backup) ":") (.getMessage ex)))))

(defn replace-with-backup!
  [^File f ^File backup]
  (try
    (verbose "Replacing original file with backup file ...")
    (.delete f)
    (io/copy backup f)
    (.delete backup)
    (catch Exception ex
      (println (red "Could not replace original file " (.getPath f) ":") (.getMessage ex)))))

;; ## Project Map

(defn read-project-map!
  [path]
  (try
    (locking read-project-map!
      (binding [*ns* (find-ns 'leiningen.core.project)]
        (load-file path)
        (when-let [project (resolve 'leiningen.core.project/project)]
          (ns-unmap 'leiningen.core.project 'project)
          @project)))
    (catch Exception ex
      (println "ERROR: Could not read project map from file:" path)
      (println "ERROR:" (.getMessage ex))))) 
