package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCombatPhaseEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Great Train Heist {R}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {2}{R} — Untap all creatures you control. If it's your combat phase, there is an
 *            additional combat phase after this phase.
 * + {2} — Creatures you control get +1/+0 and gain first strike until end of turn.
 * + {R} — Choose target opponent. Whenever a creature you control deals combat damage to
 *         that player this turn, create a tapped Treasure token.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost` (CR 702.166).
 *
 * Mode 1's extra combat is gated on [Conditions.IsInPhase] — the additional combat phase only
 * happens if it's the caster's combat phase. [AddCombatPhaseEffect] inserts an additional
 * combat phase (followed by a main phase) after the current main phase, which is the correct
 * end result for a spell cast during combat.
 *
 * Mode 3 registers a turn-scoped (non-one-shot) event-based delayed trigger via
 * [CreateDelayedTriggerEffect] whose recipient is scoped to the chosen opponent
 * (`watchedRecipient`): each time a creature the caster controls deals combat damage to that
 * specific player this turn, a tapped Treasure is created. It expires at end of turn.
 */
val GreatTrainHeist = card("Great Train Heist") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2}{R} — Untap all creatures you control. If it's your combat phase, there is an additional combat phase after this phase.\n" +
        "+ {2} — Creatures you control get +1/+0 and gain first strike until end of turn.\n" +
        "+ {R} — Choose target opponent. Whenever a creature you control deals combat damage to that player this turn, create a tapped Treasure token."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Patterns.Group.untapGroup(GroupFilter.AllCreaturesYouControl)
                        .then(
                            ConditionalEffect(
                                condition = Conditions.IsInPhase(Phase.COMBAT, yoursOnly = true),
                                effect = AddCombatPhaseEffect
                            )
                        ),
                    description = "+ {2}{R} — Untap all creatures you control. If it's your combat phase, there is an additional combat phase after this phase.",
                    additionalManaCost = "{2}{R}"
                ),
                Mode(
                    effect = Patterns.Group.modifyStatsForAll(1, 0, Filters.Group.creaturesYouControl)
                        .then(Patterns.Group.grantKeywordToAll(Keyword.FIRST_STRIKE, Filters.Group.creaturesYouControl)),
                    description = "+ {2} — Creatures you control get +1/+0 and gain first strike until end of turn.",
                    additionalManaCost = "{2}"
                ),
                Mode(
                    effect = CreateDelayedTriggerEffect(
                        trigger = Triggers.dealsDamage(
                            damageType = DamageType.Combat,
                            recipient = RecipientFilter.AnyPlayer,
                            sourceFilter = GameObjectFilter.Creature.youControl(),
                        ),
                        watchedRecipient = EffectTarget.ContextTarget(0),
                        effect = Effects.CreateTreasure(1, tapped = true),
                    ),
                    targetRequirements = listOf(Targets.Opponent),
                    description = "+ {R} — Choose target opponent. Whenever a creature you control deals combat damage to that player this turn, create a tapped Treasure token.",
                    additionalManaCost = "{R}"
                )
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/357dd9b2-5d2d-49f6-86f0-f5c4d63474dd.jpg?1712860638"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "If it isn't your combat phase as the first mode resolves, you won't get an additional combat phase. If you cast Great Train Heist before your combat phase, you won't get an additional combat phase that turn.")
    }
}
