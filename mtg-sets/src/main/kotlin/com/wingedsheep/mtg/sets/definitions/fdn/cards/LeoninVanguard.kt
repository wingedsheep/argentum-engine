package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leonin Vanguard
 * {W}
 * Creature — Cat Soldier
 * 1/1
 *
 * At the beginning of combat on your turn, if you control three or more creatures, this creature gets +1/+1 until end of turn and you gain 1 life.
 */
val LeoninVanguard = card("Leonin Vanguard") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 1
    toughness = 1
    oracleText = "At the beginning of combat on your turn, if you control three or more creatures, this creature gets +1/+1 until end of turn and you gain 1 life."

    // At the beginning of combat on your turn, if you control three or more creatures, this creature gets +1/+1 until end of turn and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.ControlCreaturesAtLeast(3)
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(1, 1, EffectTarget.Self),
                Effects.GainLife(1)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "499"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17b25850-f1bd-4410-beec-603b2a77f264.jpg?1730490494"
        flavorText = "The best fighters are skilled in both harming and healing."
    }
}
