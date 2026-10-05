package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.MtgSet

/**
 * Single source of truth for all known MTG sets.
 *
 * The catalog is auto-discovered: every [MtgSet] `object` declared anywhere under
 * [DEFINITIONS_PACKAGE] is found on the classpath via [CardDiscovery.findSets]. Adding a new set
 * is therefore just implementing [MtgSet] in its `definitions/<set>/` package — no import block to
 * extend and no list to append to (the two places that used to be easy to forget). Sets are
 * ordered chronologically by [MtgSet.releaseDate] (sets without a date sort last), then by [code]
 * for a stable, deterministic order regardless of classpath scan order.
 *
 * The game-server, gym, and tests discover sets through this catalog — no other registration
 * is required.
 */
object MtgSetCatalog {

    private const val DEFINITIONS_PACKAGE = "com.wingedsheep.mtg.sets.definitions"

    /** Sorts after any real ISO `YYYY-MM-DD` release date, so undated sets land at the end. */
    private const val UNKNOWN_DATE_SENTINEL = "9999-99-99"

    val all: List<MtgSet> by lazy {
        CardDiscovery.findSets(DEFINITIONS_PACKAGE)
            .sortedWith(compareBy({ it.releaseDate ?: UNKNOWN_DATE_SENTINEL }, { it.code }))
    }

    private val byCode: Map<String, MtgSet> by lazy { all.associateBy { it.code } }

    fun byCode(code: String): MtgSet? = byCode[code]

    fun requireByCode(code: String): MtgSet =
        byCode(code) ?: throw IllegalArgumentException("Unknown set code: $code")
}

/** The set named by [MtgSet.basicLandsFallbackCode]; an unknown code throws rather than going quiet. */
val MtgSet.basicLandsFallback: MtgSet?
    get() = basicLandsFallbackCode?.let(MtgSetCatalog::requireByCode)
