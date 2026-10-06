package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Termination Facilitator {1}{B}
 * Creature — Human Assassin
 * 1/3
 *
 * {T}: Put a bounty counter on target creature or planeswalker. Activate only as a sorcery.
 * Whenever a creature or planeswalker an opponent controls with a bounty counter on it is dealt
 * damage, destroy it.
 *
 * The second ability is an observer `isDealtDamage` trigger: the subject filter picks the damaged
 * permanent, which is the triggering entity "it" destroys. Your own bountied permanents are marked
 * but never destroyed.
 */
val TerminationFacilitator = card("Termination Facilitator") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 1
    toughness = 3
    oracleText = "{T}: Put a bounty counter on target creature or planeswalker. Activate only as a sorcery.\n" +
        "Whenever a creature or planeswalker an opponent controls with a bounty counter on it is dealt " +
        "damage, destroy it."

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.AddCounters(CounterType.BOUNTY, 1, t)
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.CreatureOrPlaneswalker.opponentControls().withCounter(CounterType.BOUNTY)
        ).isDealtDamage()
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Justine Cruz"
        flavorText = "\"You sign. They die. Simple as that.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5db59164-f0b1-4b5a-b821-ad0b2e1612d1.jpg?1783919185"
    }
}
