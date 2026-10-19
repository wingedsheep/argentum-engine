package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Invigorating Hot Spring — Kamigawa: Neon Dynasty #223 (canonical printing)
 * {1}{R}{G} · Enchantment
 *
 * This enchantment enters with four +1/+1 counters on it.
 * Modified creatures you control have haste.
 * Remove a +1/+1 counter from this enchantment: Put a +1/+1 counter on target creature you control.
 * Activate only as a sorcery and only once each turn.
 *
 * "Modified" is [StatePredicate.IsModified] (CR 700.9); the haste grant is a group static, so a
 * creature that just received a counter from the activated ability gains haste immediately. The
 * enchantment itself is not a creature, so its own counters don't matter to the grant.
 */
val InvigoratingHotSpring = card("Invigorating Hot Spring") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with four +1/+1 counters on it.\n" +
        "Modified creatures you control have haste. (Equipment, Auras you control, and counters are modifications.)\n" +
        "Remove a +1/+1 counter from this enchantment: Put a +1/+1 counter on target creature you control. " +
        "Activate only as a sorcery and only once each turn."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 4,
            selfOnly = true,
        )
    )

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified)),
        )
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59a118d1-30b8-42c0-ac42-6fba0778200a.jpg?1783923834"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling(
            "2022-02-18",
            "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.",
        )
        ruling(
            "2022-02-18",
            "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.",
        )
    }
}
