package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Repel Calamity {1}{W}
 * Instant
 *
 * Destroy target creature with power or toughness 4 or greater.
 */
val RepelCalamity = card("Repel Calamity") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Destroy target creature with power or toughness 4 or greater."

    spell {
        val creature = target(
            "creature with power or toughness 4 or greater",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerOrToughnessAtLeast(4)))
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Ryan Pancoast"
        flavorText = "The living seasons come to grow, to burn, to wither, and to freeze. As natural as it is for the Calamity Beasts to live, it is just as natural for them to die."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d068192a-6270-4981-819d-4945fa4a2b83.jpg?1721425920"
    }
}
