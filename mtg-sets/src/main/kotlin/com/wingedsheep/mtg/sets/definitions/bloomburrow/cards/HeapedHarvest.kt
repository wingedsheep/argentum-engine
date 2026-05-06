package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Heaped Harvest
 * {2}{G}
 * Artifact — Food
 *
 * When this artifact enters and when you sacrifice it, you may search your
 * library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * {2}, {T}, Sacrifice this artifact: You gain 3 life.
 */
val HeapedHarvest = card("Heaped Harvest") {
    manaCost = "{2}{G}"
    typeLine = "Artifact — Food"
    oracleText = "When this artifact enters and when you sacrifice it, you may search your library " +
        "for a basic land card, put it onto the battlefield tapped, then shuffle.\n" +
        "{2}, {T}, Sacrifice this artifact: You gain 3 life."

    // When this artifact enters, you may search for a basic land
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    // When you sacrifice it, you may search for a basic land
    triggeredAbility {
        trigger = Triggers.Sacrificed
        effect = MayEffect(
            EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    // Standard Food ability: {2}, {T}, Sacrifice this artifact: You gain 3 life.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Daniel Ljunggren"
        flavorText = "One can travel all of Valley and never eat the same meal twice."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b5349db-0e0a-4b15-886e-0db403ef49cb.jpg?1721426825"
    }
}
