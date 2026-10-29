package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
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
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frontier Siege
 * {3}{G}
 * Enchantment
 * As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of each of your main phases, add {G}{G}.
 * • Dragons — Whenever a creature you control with flying enters, you may have it fight target
 *   creature you don't control.
 *
 * The choice is recorded by [EntersWithChoice] (ChoiceType.MODE) and each anchor-word ability is
 * gated by [SourceChosenModeIs], like Outpost Siege. "Each of your main phases" is both the
 * precombat main phase and every postcombat one (additional main phases are postcombat main
 * phases, CR 505.1a), so the Khans ability is lowered to one step trigger per main-phase kind.
 * The Dragons "you may" is decided on resolution (ruling 2014-11-24).
 */
val FrontierSiege = card("Frontier Siege") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Khans or Dragons.\n" +
        "• Khans — At the beginning of each of your main phases, add {G}{G}.\n" +
        "• Dragons — Whenever a creature you control with flying enters, you may have it fight " +
        "target creature you don't control."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "khans",
                    label = "Khans",
                    description = "At the beginning of each of your main phases, add {G}{G}.",
                    iconKey = "khans"
                ),
                ModeOption(
                    id = "dragons",
                    label = "Dragons",
                    description = "Whenever a creature you control with flying enters, you may have it fight " +
                        "target creature you don't control.",
                    iconKey = "dragons"
                )
            )
        )
    )

    // Khans — precombat main phase.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        triggerRestriction = SourceChosenModeIs("khans")
        effect = Effects.AddMana(Color.GREEN, 2)
    }

    // Khans — each postcombat (and additional) main phase.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        triggerRestriction = SourceChosenModeIs("khans")
        effect = Effects.AddMana(Color.GREEN, 2)
    }

    // Dragons — a flyer you control enters: it may fight a creature you don't control.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl()).enters()
        triggerRestriction = SourceChosenModeIs("dragons")
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        optional = true
        effect = Effects.Fight(EffectTarget.TriggeringEntity, theirs)
        description = "Whenever a creature you control with flying enters, you may have it fight target " +
            "creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a474981a-581a-49c0-a036-87afaaeea7ef.jpg?1783938679"
        ruling(
            "2014-11-24",
            "The \"Khans\" ability triggers at the beginning of both your precombat main phase and your " +
                "postcombat main phase. Unused mana is lost at the end of each step and phase."
        )
        ruling(
            "2014-11-24",
            "For the \"Dragons\" ability, you decide whether the creature with flying will fight the target " +
                "creature you don't control as the ability resolves."
        )
        ruling(
            "2014-11-24",
            "If a permanent enters the battlefield as a copy of one of the Sieges, its controller will make a " +
                "new choice for that Siege. Which ability the copy has won't depend on the choice made for the original permanent."
        )
    }
}
