package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Repeated Reverberation — Core Set 2020 #156
 * {2}{R}{R} · Instant
 *
 * When you next cast an instant spell, cast a sorcery spell, or activate a loyalty ability this
 * turn, copy that spell or ability twice. You may choose new targets for the copies.
 *
 * One one-shot delayed trigger over the disjunction of the three events (`Triggers.or`): the first
 * of them consumes it. The copy reads the triggering spell or ability — still beneath the trigger
 * on the stack, or as it last existed there if it was countered (a ruling) — and prompts for new
 * targets per copy.
 */
val RepeatedReverberation = card("Repeated Reverberation") {
    manaCost = "{2}{R}{R}"
    typeLine = "Instant"
    oracleText = "When you next cast an instant spell, cast a sorcery spell, or activate a loyalty ability " +
        "this turn, copy that spell or ability twice. You may choose new targets for the copies."

    spell {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.or(
                Triggers.you.casts(GameObjectFilter.Instant),
                Triggers.you.casts(GameObjectFilter.Sorcery),
                Triggers.you.activatesAbility(loyalty = true)
            ),
            effect = Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity, DynamicAmounts.fixed(2)),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e94c783-e474-4cbc-a38a-27ad8dd15a7b.jpg?1783932972"
        ruling("2019-07-12", "Repeated Reverberation's ability will copy any instant or sorcery spell or loyalty ability, not just one with targets.")
        ruling("2019-07-12", "Copies are created even if the spell or ability that caused Repeated Reverberation's ability to trigger has been countered by the time that ability resolves. The copies resolve before the original spell or ability.")
        ruling("2019-07-12", "The copies made by Repeated Reverberation's ability are created on the stack, so they're not \"cast\" or \"activated.\" Abilities that trigger when a player casts a spell or activate an ability don't trigger.")
        ruling("2019-07-12", "Copying a loyalty ability doesn't add or remove loyalty counters from any object.")
    }
}
