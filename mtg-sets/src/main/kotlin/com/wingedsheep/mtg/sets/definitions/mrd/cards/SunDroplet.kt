package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sun Droplet — Mirrodin #249
 * {2} · Artifact
 *
 * Whenever you're dealt damage, put that many charge counters on this artifact.
 * At the beginning of each upkeep, you may remove a charge counter from this artifact.
 * If you do, you gain 1 life.
 *
 * "Whenever you're dealt damage" names no source, so it is [Triggers.YouAreDealtDamage] — every
 * source counts: a creature in combat, a burn spell, another artifact. "That many" reads the
 * damage off the trigger context ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT]), so one 5-damage hit
 * banks five counters and each instance of damage triggers separately.
 *
 * The upkeep half is "at the beginning of *each* upkeep" — it fires on every player's upkeep, and
 * the controller is always the one who decides and gains the life. The "may … if you do" pair is a
 * [MayEffect] over remove-then-gain, wrapped in a [ConditionalEffect] on there being a charge
 * counter to remove: with an empty Droplet the removal could not happen, so neither could the life
 * gain, and asking an unanswerable question every upkeep is noise rather than a choice.
 */
val SunDroplet = card("Sun Droplet") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you're dealt damage, put that many charge counters on this artifact.\n" +
        "At the beginning of each upkeep, you may remove a charge counter from this artifact. " +
        "If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.YouAreDealtDamage
        effect = Effects.AddDynamicCounters(
            counterType = Counters.CHARGE,
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            target = EffectTarget.Self,
        )
    }

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = ConditionalEffect(
            condition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.CHARGE)),
            effect = MayEffect(
                Effects.Composite(
                    RemoveCountersEffect(Counters.CHARGE, 1, EffectTarget.Self),
                    Effects.GainLife(1),
                ),
                descriptionOverride = "Remove a charge counter from Sun Droplet to gain 1 life?",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bdd01de-2ac6-4393-ad53-d52df137bc08.jpg?1783944502"
        ruling("2006-05-01", "In Two-Headed Giant, triggers only once per upkeep, not once for each player.")
    }
}
