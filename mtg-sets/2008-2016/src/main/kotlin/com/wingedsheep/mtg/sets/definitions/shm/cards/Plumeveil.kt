package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Plumeveil
 * {W/U}{W/U}{W/U}
 * Creature — Elemental
 * 4 / 4
 *
 * Flash
 * Defender, flying
 *
 * - Keyword-only creature. The keywords are declared in printed order (flash, defender, flying) so
 *   the definition mirrors the oracle text line for line.
 */
val Plumeveil = card("Plumeveil") {
    manaCost = "{W/U}{W/U}{W/U}"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "Flash\n" +
        "Defender, flying"

    keywords(Keyword.FLASH, Keyword.DEFENDER, Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Nils Hamm"
        flavorText = "\"It was vast, a great sheet of soaring wings, and equally silent. It caught us unawares and blocked our view of the kithkin stronghold.\"\n" +
            "—Grensch, merrow cutthroat"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e8b0449-3ae5-4fee-b870-af12be5e5a64.jpg?1783942736"
    }
}
