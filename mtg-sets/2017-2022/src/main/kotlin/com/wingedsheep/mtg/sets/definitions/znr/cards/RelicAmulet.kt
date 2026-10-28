package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relic Amulet — Zendikar Rising #247
 * {2} · Artifact
 *
 * Whenever you cast an instant, sorcery, or Wizard spell, put a charge counter on this artifact.
 * {2}, {T}, Remove all charge counters from this artifact: It deals that much damage to target creature.
 *
 * "That much" is the number of counters the cost removed — they're already gone by resolution, so
 * the damage reads [DynamicAmounts.countersRemovedAsCost], not what the Amulet holds then.
 */
val RelicAmulet = card("Relic Amulet") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you cast an instant, sorcery, or Wizard spell, put a charge counter on this artifact.\n" +
        "{2}, {T}, Remove all charge counters from this artifact: It deals that much damage to target creature."

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery or GameObjectFilter.Any.withSubtype(Subtype.WIZARD)
        )
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "Whenever you cast an instant, sorcery, or Wizard spell, put a charge counter on " +
            "this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.RemoveAllCountersFromSelf(CounterType.CHARGE),
        )
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.countersRemovedAsCost(), t)
        description = "{2}, {T}, Remove all charge counters from this artifact: It deals that much " +
            "damage to target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Yeong-Hao Han"
        flavorText = "Old power finds new purpose."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ab97a84-fe8c-4986-afd8-3abdcc029f7f.jpg?1783929314"
        ruling("2020-09-25", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger, but after targets have been chosen for that spell (if it has any targets). The ability resolves even if that spell is countered.")
        ruling("2020-09-25", "A Wizard spell is one with the creature type Wizard. Spells that are Wizard-themed (such as Relic Amulet) aren't Wizard spells.")
    }
}
