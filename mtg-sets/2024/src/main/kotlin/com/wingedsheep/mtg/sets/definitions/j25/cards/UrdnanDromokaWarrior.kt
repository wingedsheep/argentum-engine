package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Urdnan, Dromoka Warrior — Foundations Jumpstart #34 (canonical printing)
 * {1}{W} · Legendary Creature — Human Warrior · 1/1
 *
 * When Urdnan enters, put a +1/+1 counter on target creature.
 * Whenever you attack, target attacking creature with a +1/+1 counter on it gains first strike
 * until end of turn. If that creature has two or more +1/+1 counters on it, it gains double
 * strike until end of turn instead.
 *
 * "Whenever you attack" is the once-per-declaration `YouAttackEvent` (Mishra, Claimed by Gix).
 * The "two or more" check is made as the trigger resolves, not when it's targeted, so it's an
 * `If` over the target's counters at resolution: double strike when it has two, first strike
 * otherwise — never both, matching "instead".
 */
val UrdnanDromokaWarrior = card("Urdnan, Dromoka Warrior") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "When Urdnan enters, put a +1/+1 counter on target creature.\n" +
        "Whenever you attack, target attacking creature with a +1/+1 counter on it gains first strike until end of turn. " +
        "If that creature has two or more +1/+1 counters on it, it gains double strike until end of turn instead."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val attacker = target(
            TargetFilter(GameObjectFilter.Creature.attacking().withCounter(CounterType.PLUS_ONE_PLUS_ONE))
        )
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE, atLeast = 2),
                attacker
            ),
            then = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, attacker),
            otherwise = Effects.GrantKeyword(Keyword.FIRST_STRIKE, attacker)
        )
        description = "Whenever you attack, target attacking creature with a +1/+1 counter on it gains first " +
            "strike until end of turn. If it has two or more +1/+1 counters on it, it gains double strike instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Yakotakos"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f239682a-2b81-4f62-a474-60e8db343200.jpg?1783908860"
    }
}
