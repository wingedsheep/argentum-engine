package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sanguine Spy
 * {2}{B}
 * Creature — Vampire Rogue
 * 2/3
 *
 * Menace, lifelink
 * {1}, Sacrifice another creature: Surveil 1.
 * At the beginning of your end step, if there are five or more mana values among cards in your
 * graveyard, you may pay 2 life. If you do, draw a card.
 *
 * The end-step clause is an intervening if ([Conditions.DistinctManaValuesInGraveyard]), checked
 * when the trigger would fire and again on resolution.
 */
val SanguineSpy = card("Sanguine Spy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 2
    toughness = 3
    oracleText = "Menace, lifelink\n" +
        "{1}, Sacrifice another creature: Surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)\n" +
        "At the beginning of your end step, if there are five or more mana values among cards in your graveyard, you may pay 2 life. If you do, draw a card."

    keywords(Keyword.MENACE, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Patterns.Library.surveil(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.DistinctManaValuesInGraveyard(5)
        effect = Effects.MayPay(
            cost = Effects.PayLife(2),
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d52ea1e-5b66-47c7-9a6d-95f5996565d8.jpg?1783923124"
    }
}
