package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serum-Core Chimera — Phyrexia: All Will Be One #215
 * {2}{U}{R} · Creature — Phyrexian Chimera · 2/4 · Uncommon
 *
 * Flying
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * Remove three oil counters from this creature: Draw a card. Then you may discard a nonland card.
 * When you discard a card this way, this creature deals 3 damage to target creature or
 * planeswalker. Activate only as a sorcery.
 *
 * The discard is the action of a reflexive trigger (CR 603.12): the damage target is chosen only
 * when the reflexive ability goes on the stack, after a nonland card was actually discarded.
 */
val SerumCoreChimera = card("Serum-Core Chimera") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Phyrexian Chimera"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "Remove three oil counters from this creature: Draw a card. Then you may discard a nonland " +
        "card. When you discard a card this way, this creature deals 3 damage to target creature or " +
        "planeswalker. Activate only as a sorcery."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "Whenever you cast a noncreature spell, put an oil counter on this creature."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.OIL, 3)
        timing = TimingRule.SorcerySpeed
        effect = Effects.DrawCards(1, EffectTarget.Controller) then
            Effects.ReflexiveTrigger(
                action = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Nonland),
                optional = true,
            ) {
                val creatureOrPlaneswalker = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(3, creatureOrPlaneswalker, damageSource = EffectTarget.Self)
            }
        description = "Remove three oil counters from this creature: Draw a card. Then you may discard " +
            "a nonland card. When you discard a card this way, this creature deals 3 damage to target " +
            "creature or planeswalker. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31147651-9a5e-4329-923b-e464f67135e9.jpg?1783917997"
        ruling(
            "2023-02-04",
            "You don't choose a target for Serum-Core Chimera's last ability at the time you activate it. " +
                "Rather, a second \"reflexive\" ability triggers when you discard a card this way. You choose " +
                "a target for that ability as it goes on the stack. Each player may respond to this " +
                "triggered ability as normal."
        )
    }
}
