package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trash the Town {G}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {2} — Put two +1/+1 counters on target creature.
 * + {1} — Target creature gains trample until end of turn.
 * + {1} — Until end of turn, target creature gains "Whenever this creature deals combat
 *   damage to a player, draw two cards."
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`
 * and per-mode `additionalManaCost` (CR 702.166): at least one mode must be chosen, no mode
 * twice, each with its own additional cost. Every mode targets a creature independently
 * (its own `ContextTarget(0)`).
 *
 * The third mode grants a triggered ability until end of turn ([GrantTriggeredAbilityEffect]
 * defaults to [com.wingedsheep.sdk.scripting.Duration.EndOfTurn]); the granted
 * "deals combat damage to a player → draw two cards" ability draws for the creature's
 * controller ([EffectTarget.Controller]).
 */
val TrashTheTown = card("Trash the Town") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2} — Put two +1/+1 counters on target creature.\n" +
        "+ {1} — Target creature gains trample until end of turn.\n" +
        "+ {1} — Until end of turn, target creature gains \"Whenever this creature deals " +
        "combat damage to a player, draw two cards.\""

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.AddCounters("+1/+1", 2, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {2} — Put two +1/+1 counters on target creature.",
                    additionalManaCost = "{2}"
                ),
                Mode(
                    effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {1} — Target creature gains trample until end of turn.",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = GrantTriggeredAbilityEffect(
                        ability = TriggeredAbility.create(
                            trigger = Triggers.DealsCombatDamageToPlayer.event,
                            binding = Triggers.DealsCombatDamageToPlayer.binding,
                            effect = Effects.DrawCards(2)
                        ),
                        target = EffectTarget.ContextTarget(0)
                    ),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {1} — Until end of turn, target creature gains \"Whenever " +
                        "this creature deals combat damage to a player, draw two cards.\"",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/eda59f99-1d6d-4051-ac89-b7cbfa19262e.jpg?1712860614"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "Each chosen mode requires a target. You may choose the same creature as the target for multiple modes.")
        ruling("2024-04-12", "The mana value of a spell with spree is determined only by its mana cost. It doesn't matter which modes you choose or which additional costs you pay.")
    }
}
