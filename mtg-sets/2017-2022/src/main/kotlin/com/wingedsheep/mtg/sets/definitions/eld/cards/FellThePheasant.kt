package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fell the Pheasant
 * {1}{G}
 * Instant
 *
 * Fell the Pheasant deals 5 damage to target creature with flying. Create a Food token.
 */
val FellThePheasant = card("Fell the Pheasant") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Fell the Pheasant deals 5 damage to target creature with flying. Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val t = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.DealDamage(5, t) then Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Randy Gallegos"
        flavorText = "\"A keen eye and steady hand are essential for defeating most foes, including hunger.\"\n—Syr Grenphire, knight of Garenbrig"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33d3cc84-7cb6-4de2-9018-4695a3b1e099.jpg?1783932611"
    }
}
