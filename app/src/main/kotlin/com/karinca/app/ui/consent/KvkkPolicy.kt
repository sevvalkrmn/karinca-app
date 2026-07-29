package com.karinca.app.ui.consent

// Bump this (and the copy shown in KvkkConsentScreen) whenever either consent item's
// real text materially changes — anyone consented under an older version is re-prompted.
object KvkkPolicy {
    // v2: real "Finansal Verilerin Saklanması Aydınlatma Metni" landed for the data
    // storage item too — both consent items now show real KVKK text.
    const val CURRENT_VERSION = "v2-both-disclosures"
}
