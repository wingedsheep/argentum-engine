package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Consuming Fervor
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3 and has "At the beginning of your upkeep, put a -1/-1 counter on
 * this creature."
 *
 * The upkeep trigger is *granted* to the creature (the [InstillFuror] shape), so "your upkeep" is
 * the creature controller's upkeep and "this creature" is the enchanted creature itself.
 */
val ConsumingFervor = card("Consuming Fervor") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/+3 and has \"At the beginning of your upkeep, put a -1/-1 counter on this creature.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self),
                descriptionOverride = "At the beginning of your upkeep, put a -1/-1 counter on this creature.",
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Svetlin Velinov"
        flavorText = "Headlong is the only way to pass through the gate to the afterlife."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d529d230-89b0-453b-987b-a47e15bc3830.jpg"
    }
}
