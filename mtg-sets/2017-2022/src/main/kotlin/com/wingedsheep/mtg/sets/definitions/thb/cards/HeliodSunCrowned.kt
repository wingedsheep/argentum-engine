package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heliod, Sun-Crowned — Theros Beyond Death #18
 * {2}{W} · Legendary Enchantment Creature — God · 5/5
 *
 * Indestructible
 * As long as your devotion to white is less than five, Heliod isn't a creature.
 * Whenever you gain life, put a +1/+1 counter on target creature or enchantment you control.
 * {1}{W}: Another target creature gains lifelink until end of turn.
 *
 * The Theros god clause is the Nylea shape: a Layer 4 [RemoveCardType] of CREATURE on Heliod
 * himself, gated by `DevotionTo(WHITE) < 5`. The life-gain trigger fires once per life-gain event
 * (rulings), and Heliod may target himself.
 */
val HeliodSunCrowned = card("Heliod, Sun-Crowned") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to white is less than five, Heliod isn't a creature.\n" +
        "Whenever you gain life, put a +1/+1 counter on target creature or enchantment you control.\n" +
        "{1}{W}: Another target creature gains lifelink until end of turn."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.WHITE), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        val t = target(TargetFilter.CreatureOrEnchantment.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Whenever you gain life, put a +1/+1 counter on target creature or enchantment you control."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, creature)
        description = "{1}{W}: Another target creature gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01a8576e-cadc-4521-aadd-3a05f0bc4d20.jpg?1783931597"
        ruling(
            "2020-01-24",
            "Heliod's triggered ability triggers just once for each life-gaining event, whether it's 1 life from " +
                "Daxos, Blessed by the Sun or 3 life from Cling to Dust."
        )
        ruling("2020-01-24", "Heliod can be the target of its own triggered ability.")
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
