package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Citadel Siege
 * {2}{W}{W}
 * Enchantment
 *
 * As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of combat on your turn, put two +1/+1 counters on target creature
 *   you control.
 * • Dragons — At the beginning of combat on each opponent's turn, tap target creature that player
 *   controls.
 *
 * The choice is recorded by [EntersWithChoice] (ChoiceType.MODE) and each anchor-word ability is
 * gated by [SourceChosenModeIs], like Monastery Siege. For the Dragons trigger, "that player" is the
 * active opponent — a step trigger's triggering player — so the target filter reads
 * `controlledByTriggeringPlayer()` (The Abyss shape).
 */
val CitadelSiege = card("Citadel Siege") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Khans or Dragons.\n" +
        "• Khans — At the beginning of combat on your turn, put two +1/+1 counters on target creature you control.\n" +
        "• Dragons — At the beginning of combat on each opponent's turn, tap target creature that player controls."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "khans",
                    label = "Khans",
                    description = "At the beginning of combat on your turn, put two +1/+1 counters on " +
                        "target creature you control.",
                    iconKey = "khans"
                ),
                ModeOption(
                    id = "dragons",
                    label = "Dragons",
                    description = "At the beginning of combat on each opponent's turn, tap target " +
                        "creature that player controls.",
                    iconKey = "dragons"
                )
            )
        )
    )

    // Khans — two +1/+1 counters on a creature you control at the beginning of your combat.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        triggerRestriction = SourceChosenModeIs("khans")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 2,
            target = creature
        )
        description = "At the beginning of combat on your turn, put two +1/+1 counters on target " +
            "creature you control."
    }

    // Dragons — tap a creature the active opponent controls at the beginning of their combat.
    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.BEGIN_COMBAT)
        triggerRestriction = SourceChosenModeIs("dragons")
        val creature = target(TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()))
        effect = Effects.Tap(creature)
        description = "At the beginning of combat on each opponent's turn, tap target creature that " +
            "player controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/239c373a-465c-4cda-9895-624871d8606e.jpg?1783938714"
        ruling(
            "2014-11-24",
            "If a permanent enters the battlefield as a copy of one of the Sieges, its controller will make a " +
                "new choice for that Siege. Which ability the copy has won't depend on the choice made for the original permanent."
        )
    }
}
