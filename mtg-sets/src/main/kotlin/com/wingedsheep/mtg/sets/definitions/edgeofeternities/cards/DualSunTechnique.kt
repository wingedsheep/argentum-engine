package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Dual-Sun Technique
 * {1}{W}
 * Instant
 * Target creature you control gains double strike until end of turn. If it has a +1/+1 counter on it, draw a card.
 */
val DualSunTechnique = card("Dual-Sun Technique") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Target creature you control gains double strike until end of turn. If it has a +1/+1 counter on it, draw a card."

    // Main spell effect
    spell {
        val target = target("target creature you control", Targets.CreatureYouControl)
        
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, target)
            .then(
                // Draw a card if the target has a +1/+1 counter
                ConditionalEffect(
                    condition = Conditions.TargetHasCounter(CounterTypeFilter.PlusOnePlusOne),
                    effect = Effects.DrawCards(1, com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Ioannis Fiore"
        flavorText = "\"Haste begets blunder. Be patient, Aora. As the sun rises, you will yet earn your second blade.\"\n—Master Iridiss"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8931132-391f-4f16-b480-0a245ab2ec21.jpg?1752946606"
    }
}
