package com.wingedsheep.sdk.model

import kotlinx.serialization.Serializable

/**
 * One specific printing of a card.
 *
 * Multiple `Printing`s can share an `oracleId` — that's the whole point. The engine treats
 * cards by oracle identity (name / `CardDefinition`); printings are a presentation +
 * deckbuilding concern that carry per-print art, set, collector number, and other Scryfall
 * metadata. Decks pick a `Printing` via [PrintingRef]; the engine looks the chosen printing
 * up at game-init and stamps the printing's image URLs onto the per-entity `CardComponent`.
 *
 * The triple `(oracleId, setCode, collectorNumber)` is the natural key. `(setCode,
 * collectorNumber)` alone is unique within Scryfall and is what [PrintingRef] carries.
 */
@Serializable
data class Printing(
    val oracleId: String,
    val name: String,
    val setCode: String,
    val collectorNumber: String,
    val scryfallId: String? = null,
    val artist: String? = null,
    val imageUri: String? = null,
    val backFaceImageUri: String? = null,
    val releaseDate: String? = null,
    val rarity: Rarity = Rarity.COMMON,
    val isPromo: Boolean = false,
    val isFullArt: Boolean = false,
    val frameEffects: List<String> = emptyList(),
    /** Scryfall `border_color` (e.g. "black", "white", "borderless"). Null when unknown. */
    val borderColor: String? = null,
    /**
     * Scryfall `printed_name` — the name printed on this card when it differs from the oracle
     * [name], e.g. Through the Omenpaths' in-universe names for Marvel's Spider-Man cards.
     * Presentation only: rules identity stays [name]. Null when the printing uses the oracle name.
     */
    val printedName: String? = null,
) {
    /** Reference form for decks and lookup APIs. */
    val ref: PrintingRef get() = PrintingRef(setCode, collectorNumber)

    /**
     * True when this printing uses an alternate frame treatment — a *showcase* frame
     * (`"showcase" in frameEffects`) or a *borderless* border (`borderColor == "borderless"`).
     *
     * This is the predicate the booster generator uses to pick "special art" printings for its
     * variant slot, and what the deckbuilder picker can use to label a printing as a special
     * version. It deliberately does **not** count plain full-art or promo treatments — only the
     * showcase/borderless families a player perceives as alternate art.
     */
    val isAlternateFrame: Boolean
        get() = borderColor == "borderless" || "showcase" in frameEffects
}
