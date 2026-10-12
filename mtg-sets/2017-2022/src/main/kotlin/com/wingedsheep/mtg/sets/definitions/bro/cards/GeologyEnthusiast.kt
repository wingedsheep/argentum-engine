package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Geology Enthusiast
 * {3}{U}{U}
 * Creature — Human Artificer
 * 2/5
 * At the beginning of your end step, create a tapped Powerstone token.
 * {6}: Draw a card and put a +1/+1 counter on this creature.
 */
val GeologyEnthusiast = card("Geology Enthusiast") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 5
    oracleText = "At the beginning of your end step, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")\n" +
        "{6}: Draw a card and put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreatePowerstone(tapped = true)
    }

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.DrawCards(1) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbb1f21c-60fa-4fcc-844b-25bf5b47fd1f.jpg?1783919995"
    }
}
