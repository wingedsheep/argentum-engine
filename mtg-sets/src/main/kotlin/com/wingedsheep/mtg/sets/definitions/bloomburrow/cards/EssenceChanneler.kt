package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Essence Channeler
 * {1}{W}
 * Creature — Bat Cleric
 * 2/1
 *
 * As long as you've lost life this turn, this creature has flying and vigilance.
 * Whenever you gain life, put a +1/+1 counter on this creature.
 * When this creature dies, put its counters on target creature you control.
 */
val EssenceChanneler = card("Essence Channeler") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Bat Cleric"
    power = 2
    toughness = 1
    oracleText = "As long as you've lost life this turn, this creature has flying and vigilance.\nWhenever you gain life, put a +1/+1 counter on this creature.\nWhen this creature dies, put its counters on target creature you control."

    // Conditional flying while you've lost life this turn
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, StaticTarget.SourceCreature),
            condition = Conditions.YouLostLifeThisTurn
        )
    }

    // Conditional vigilance while you've lost life this turn
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, StaticTarget.SourceCreature),
            condition = Conditions.YouLostLifeThisTurn
        )
    }

    // Whenever you gain life, put a +1/+1 counter on this creature
    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // When this creature dies, put its counters on target creature you control
    // Uses LastKnownCounterCount to capture +1/+1 counters from before death
    triggeredAbility {
        trigger = Triggers.Dies
        target = Targets.CreatureYouControl
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.LastKnownCounterCount,
            EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5aaf7e4c-4d5d-4acc-a834-e6c4a7629408.jpg?1724003311"

        ruling("2024-07-26", "Essence Channeler's first ability cares whether you've lost life this turn, not how your life total has changed.")
        ruling("2024-07-26", "Essence Channeler's second ability triggers just once for each life-gaining event, no matter how much life was gained.")
        ruling("2024-07-26", "Essence Channeler's last ability puts all counters that were on Essence Channeler onto the target creature, not just its +1/+1 counters.")
    }
}
