package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Triad of Fates — Theros #206
 * {2}{W}{B} · Legendary Creature — Human Wizard · 3/3
 *
 * {1}, {T}: Put a fate counter on another target creature.
 * {W}, {T}: Exile target creature that has a fate counter on it, then return it to the battlefield
 * under its owner's control.
 * {B}, {T}: Exile target creature that has a fate counter on it. Its controller draws two cards.
 *
 * The white ability is Momentary Blink's exile-then-move shape; the returned creature is a new
 * object, so it comes back without the fate counter. The black ability's draw goes to
 * [EffectTarget.TargetController], the controller the exiled creature last had on the battlefield
 * (Introduction to Annihilation's shape).
 */
val TriadOfFates = card("Triad of Fates") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "{1}, {T}: Put a fate counter on another target creature.\n" +
        "{W}, {T}: Exile target creature that has a fate counter on it, then return it to the battlefield under its owner's control.\n" +
        "{B}, {T}: Exile target creature that has a fate counter on it. Its controller draws two cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.AddCounters(CounterType.FATE, 1, creature)
        description = "{1}, {T}: Put a fate counter on another target creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val creature = target(TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.FATE)))
        effect = Effects.Exile(creature) then Effects.Move(creature, Zone.BATTLEFIELD)
        description = "{W}, {T}: Exile target creature that has a fate counter on it, then return it to the " +
            "battlefield under its owner's control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val creature = target(TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.FATE)))
        effect = Effects.Exile(creature) then Effects.DrawCards(2, EffectTarget.TargetController)
        description = "{B}, {T}: Exile target creature that has a fate counter on it. Its controller draws two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/588a8f90-4344-4882-9671-4c7295186a58.jpg?1783939723"
        ruling(
            "2013-09-15",
            "The creature that returns to the battlefield during the resolution of Triad of the Fate's middle " +
                "ability does so as a new object, with no memory of its previous existence. It won't have a fate " +
                "counter on it."
        )
    }
}
