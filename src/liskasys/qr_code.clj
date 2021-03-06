(ns liskasys.qr-code
  (:import [cz.geek.spayd CzechBankAccount CzechSpaydPayment SpaydQRFactory]
           java.io.File))

(defn save-qr-code [account amount var-symbol recipient msg]
  (let [payment (doto (CzechSpaydPayment. (CzechBankAccount. account))
                  (.setAmount amount)
                  (.setVs var-symbol)
                  (.setRecipientName recipient)
                  (.setMessage msg))
        tmp-file (File/createTempFile "qrcode" ".png")]
    (doto (SpaydQRFactory.)
      (.saveQRCode payment "png" tmp-file))
    tmp-file))
