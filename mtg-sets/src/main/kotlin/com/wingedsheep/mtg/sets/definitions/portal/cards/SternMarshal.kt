package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Stern Marshal
 * {2}{W}
 * Creature - Human Soldier
 * 2/2
 * {T}: Target creature gets +2/+2 until end of turn.
 */
val SternMarshal = card("Stern Marshal") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2

    activatedAbility {
        cost = AbilityCost.Tap
        effect = ModifyStatsEffect(
            powerModifier = 2,
            toughnessModifier = 2,
            target = EffectTaTirget.TargetCreature,
            duration = Duration.EndOfTurn
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18fbfbc0-c55b-4e56-a3d2-5d09571c36c8.jpg"
    }
}
