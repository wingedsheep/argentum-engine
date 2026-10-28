package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gemstone Caverns — Time Spiral #274
 * Legendary Land
 *
 * If this card is in your opening hand and you're not the starting player, you may begin the game
 * with Gemstone Caverns on the battlefield with a luck counter on it. If you do, exile a card from
 * your hand.
 * {T}: Add {C}. If Gemstone Caverns has a luck counter on it, instead add one mana of any color.
 *
 * "You're not the starting player" is `Conditions.IsNotYourTurn` — see `BeginOnBattlefield.onlyIf`.
 */
val GemstoneCaverns = card("Gemstone Caverns") {
    typeLine = "Legendary Land"
    colorIdentity = ""
    oracleText = "If this card is in your opening hand and you're not the starting player, you may begin the game " +
        "with Gemstone Caverns on the battlefield with a luck counter on it. If you do, exile a card from your hand.\n" +
        "{T}: Add {C}. If Gemstone Caverns has a luck counter on it, instead add one mana of any color."

    mayBeginGameOnBattlefield(
        onlyIf = Conditions.IsNotYourTurn,
        counters = mapOf(CounterType.LUCK to 1),
        then = Patterns.Hand.exileFromHand(1, EffectTarget.Controller),
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.If(
            condition = Conditions.SourceHasCounter(CounterType.LUCK),
            then = Effects.AddAnyColorMana(),
            otherwise = Effects.AddColorlessMana(1),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. If Gemstone Caverns has a luck counter on it, instead add one mana of any color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "274"
        artist = "Martina Pilcerova"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94d74254-4750-4fb3-9e53-473a5f98b315.jpg?1783943194"
    }
}
