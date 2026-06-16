package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Shackle Slinger
 * {2}{U}
 * Creature — Human Soldier
 * 3/2
 *
 * Whenever you cast your second spell each turn, choose target creature an opponent controls. If
 * it's tapped, put a stun counter on it. Otherwise, tap it.
 *
 * "Your second spell each turn" → [Triggers.NthSpellCast] with n = 2 scoped to [Player.You]. The
 * stun-or-tap branch is a resolution-time read of the target's tapped state via
 * [Conditions.TargetIsTapped]: tapped targets get a stun counter, untapped targets are tapped.
 */
val ShackleSlinger = card("Shackle Slinger") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Whenever you cast your second spell each turn, choose target creature an opponent " +
        "controls. If it's tapped, put a stun counter on it. Otherwise, tap it. (If a permanent " +
        "with a stun counter would become untapped, remove one from it instead.)"

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.opponentControls()))
        effect = ConditionalEffect(
            condition = Conditions.TargetIsTapped(),
            effect = AddCountersEffect(counterType = Counters.STUN, count = 1, target = t),
            elseEffect = Effects.Tap(t),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Josh Hass"
        flavorText = "Even when he missed the wrists, a pair of thundercuffs to the head got the job done."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6cfe383-e483-47e7-99d1-991a06b089bc.jpg?1712355493"
    }
}
