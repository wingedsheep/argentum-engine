package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nameless Inversion
 * {1}{B}
 * Kindred Instant — Shapeshifter
 *
 * Changeling (This card is every creature type.)
 * Target creature gets +3/-3 and loses all creature types until end of turn.
 *
 * Note: "Tribal" was errata'd to "Kindred" in 2024.
 */
val NamelessInversion = card("Nameless Inversion") {
    manaCost = "{1}{B}"
    typeLine = "Kindred Instant — Shapeshifter"
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Target creature gets +3/-3 and loses all creature types until end of turn."

    keywords(Keyword.CHANGELING)

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(3, -3, creature) then
            Effects.LoseAllCreatureTypes(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Dominik Mayer"
        flavorText = "Eclipsed realms are the spaces between Lorwyn's stability and Shadowmoor's chaos—raw and undefined boundaries where the plane naturally corrects imbalances in power and influence."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a4944ac-839e-4aa1-8200-78cff36bb2ed.jpg?1767871890"
    }
}
