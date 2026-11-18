package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Eternal Thirst — Magic 2015 #95
 * {1}{B} · Enchantment — Aura · Common
 *
 * Enchant creature
 * Enchanted creature has lifelink and "Whenever a creature an opponent controls dies, put a +1/+1
 * counter on this creature."
 *
 * The quoted ability is *granted to the host* (default self-scoped [GrantTriggeredAbility]), not
 * printed on the Aura, so two words read from the enchanted creature's side: "this creature" is
 * [EffectTarget.Self] — the host the ability was granted to — and "an opponent" is an opponent of
 * the host's controller. Put on an opposing creature, it grows when *your* creatures die.
 */
val EternalThirst = card("Eternal Thirst") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has lifelink and \"Whenever a creature an opponent controls dies, " +
        "put a +1/+1 counter on this creature.\" (Damage dealt by a creature with lifelink also " +
        "causes its controller to gain that much life.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies(),
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                descriptionOverride = "Whenever a creature an opponent controls dies, " +
                    "put a +1/+1 counter on this creature."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9228ea53-4aea-45f5-bec5-4a0629ec8825.jpg?1783939184"
        ruling(
            "2017-11-17",
            "If the enchanted creature is dealt lethal damage at the same time as a creature an " +
                "opponent controls, they're destroyed at the same time. It won't receive a +1/+1 " +
                "counter from its ability in time to save it."
        )
    }
}
