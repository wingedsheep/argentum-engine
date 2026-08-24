package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Trip Wire
 * {2}{G}
 * Sorcery
 *
 * Portal Three Kingdoms' answer to horsemanship: a plain destroy whose target filter carries the
 * keyword predicate.
 */
val TripWire = card("Trip Wire") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature with horsemanship."

    spell {
        val t = target(
            "target",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.HORSEMANSHIP))
            )
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Hong Yan"
        flavorText = "Trip wire, hooked poles, and sunken pits were commonly used to unhorse riders during the Three Kingdoms period."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eb1e16f-002e-4a81-ba41-cfe41f3a9071.jpg"
    }
}
