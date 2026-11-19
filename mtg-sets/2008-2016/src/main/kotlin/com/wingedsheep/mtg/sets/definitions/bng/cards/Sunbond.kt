package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sunbond — Born of the Gods #28
 * {3}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "Whenever you gain life, put that many +1/+1 counters on this creature."
 *
 * The trigger is granted to the enchanted creature, so "you" is the creature's controller and
 * "this creature" is the creature itself.
 */
val Sunbond = card("Sunbond") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever you gain life, put that many +1/+1 counters on this creature.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.gainsLife(),
                effect = Effects.AddDynamicCounters(
                    counterType = CounterType.PLUS_ONE_PLUS_ONE,
                    amount = DynamicAmounts.triggerLifeGained(),
                    target = EffectTarget.Self,
                ),
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Noah Bradley"
        flavorText = "\"I was not chosen for my faith in the gods. Sometimes the gods must put their faith in us.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f14bbff-8a3c-4aa0-a5aa-0dd6cb23b2a3.jpg?1783939574"
    }
}
