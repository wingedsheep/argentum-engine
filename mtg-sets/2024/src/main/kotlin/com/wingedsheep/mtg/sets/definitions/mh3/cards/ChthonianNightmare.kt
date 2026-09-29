package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val ChthonianNightmare = card("Chthonian Nightmare") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you get {E}{E}{E} (three energy counters).\nPay X {E}, Sacrifice a creature, Return this enchantment to its owner's hand: Return target creature card with mana value X from your graveyard to the battlefield. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }
    activatedAbility {
        cost = Costs.Composite(
            Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmounts.xValue()),
            Costs.Sacrifice(Filters.Creature),
            Costs.ReturnSelfToHand,
        )
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureInYourGraveyard.manaValueEqualsX())
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce5dd2c1-b6e0-4914-b5c9-7dd451c29e22.jpg?1783911283"
        ruling("2024-06-07", "Chthonian Nightmare's last ability must have a target to be activated. You can't activate it without a target in order to return Chthonian Nightmare to your hand.")
        ruling("2024-06-07", "If a card in your graveyard has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}
