package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Palace Siege
 * {3}{B}{B}
 * Enchantment
 *
 * As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of your upkeep, return target creature card from your graveyard to
 *   your hand.
 * • Dragons — At the beginning of your upkeep, each opponent loses 2 life and you gain 2 life.
 *
 * The choice is recorded by [EntersWithChoice] (ChoiceType.MODE) and each anchor-word ability is
 * gated by [SourceChosenModeIs], like the other Fate Reforged Sieges.
 */
val PalaceSiege = card("Palace Siege") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Khans or Dragons.\n" +
        "• Khans — At the beginning of your upkeep, return target creature card from your graveyard to your hand.\n" +
        "• Dragons — At the beginning of your upkeep, each opponent loses 2 life and you gain 2 life."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "khans",
                    label = "Khans",
                    description = "At the beginning of your upkeep, return target creature card from your " +
                        "graveyard to your hand.",
                    iconKey = "khans"
                ),
                ModeOption(
                    id = "dragons",
                    label = "Dragons",
                    description = "At the beginning of your upkeep, each opponent loses 2 life and you gain 2 life.",
                    iconKey = "dragons"
                )
            )
        )
    )

    // Khans — regrow a creature card at your upkeep.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerRestriction = SourceChosenModeIs("khans")
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creatureCard, Zone.HAND)
        description = "At the beginning of your upkeep, return target creature card from your graveyard " +
            "to your hand."
    }

    // Dragons — drain each opponent for 2 at your upkeep.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerRestriction = SourceChosenModeIs("dragons")
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2, EffectTarget.Controller)
        description = "At the beginning of your upkeep, each opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9ee5a83-7096-4cbd-ad16-eb839d293d88.jpg?1783938695"
        ruling(
            "2014-11-24",
            "If a permanent enters the battlefield as a copy of one of the Sieges, its controller will make a " +
                "new choice for that Siege. Which ability the copy has won't depend on the choice made for the original permanent."
        )
    }
}
