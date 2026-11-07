package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ainok Guide
 * {1}{G}
 * Creature — Dog Scout
 * 1/1
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Search your library for a basic land card, reveal it, then shuffle and put that card on top.
 */
val AinokGuide = card("Ainok Guide") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Scout"
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Search your library for a basic land card, reveal it, then shuffle and put that card on top."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            Mode.noTarget(
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    destination = SearchDestination.TOP_OF_LIBRARY,
                    reveal = true
                ),
                "Search your library for a basic land card, then shuffle and put that card on top of your library",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c361ea9c-a989-4ded-8b2c-f565aa9d8a1d.jpg?1783938682"
    }
}
