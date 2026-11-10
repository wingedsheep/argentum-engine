package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tears of Valakut
 * {1}{R}
 * Instant
 * This spell can't be countered.
 * Tears of Valakut deals 5 damage to target creature with flying.
 */
val TearsOfValakut = card("Tears of Valakut") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "Tears of Valakut deals 5 damage to target creature with flying."
    cantBeCountered = true

    spell {
        val flier = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
        effect = Effects.DealDamage(5, flier)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Noah Bradley"
        flavorText = "When Kozilek destroyed Sea Gate, distant Valakut could not contain its grief."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db858823-ae8b-4da1-be6f-aee541e008ae.jpg"
    }
}
