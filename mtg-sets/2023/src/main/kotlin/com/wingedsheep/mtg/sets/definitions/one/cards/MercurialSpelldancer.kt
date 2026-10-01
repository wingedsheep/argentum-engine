package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mercurial Spelldancer
 * {1}{U}
 * Creature — Phyrexian Rogue
 * 2/1
 *
 * This creature can't be blocked.
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * Whenever this creature deals combat damage to a player, you may remove two oil counters from it.
 * If you do, when you next cast an instant or sorcery spell this turn, copy that spell. You may
 * choose new targets for the copy.
 *
 * "Remove two" is all-or-nothing: with fewer than two oil counters the removal can't be done, so the
 * may is only offered when the creature carries at least two (the counter executor would otherwise
 * take off a lone counter and report success). The payoff is a one-shot delayed trigger (as
 * Complete the Circuit) that expires at end of turn, copying the triggering spell.
 */
val MercurialSpelldancer = card("Mercurial Spelldancer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Rogue"
    power = 2
    toughness = 1
    oracleText = "This creature can't be blocked.\n" +
        "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "Whenever this creature deals combat damage to a player, you may remove two oil counters " +
        "from it. If you do, when you next cast an instant or sorcery spell this turn, copy that " +
        "spell. You may choose new targets for the copy."

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.If(
            condition = Conditions.SourceCounterCountAtLeast(CounterType.OIL, 2),
            then = Effects.May(
                Effects.RemoveCounters(CounterType.OIL, 2, EffectTarget.Self) then
                    Effects.CreateDelayedTrigger(
                        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery),
                        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
                        fireOnce = true,
                        expiry = DelayedTriggerExpiry.EndOfTurn,
                    ),
                descriptionOverride = "Remove two oil counters to copy the next instant or sorcery you cast this turn?",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Marcela Bolívar"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf28c75d-1fb3-44cc-b651-5b2830e22add.jpg?1783918060"
    }
}
