package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hinterland Hermit // Hinterland Scourge (Dark Ascension #94)
 *
 * The Innistrad werewolf transform pair (Afflicted Deserter's shape); the back's "must be blocked
 * if able" is the at-least-one [MustBeBlocked] static (Fibrous Entangler).
 */
private val HinterlandHermitFront = card("Hinterland Hermit") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 1
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Steven Belledin"
        flavorText = "He tried to live far from those he could hurt . . ."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6edac85-78e7-4e90-b538-b67c88bb5c62.jpg?1783940823"
    }
}

private val HinterlandScourge = card("Hinterland Scourge") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 2
    oracleText = "This creature must be blocked if able.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    staticAbility {
        ability = MustBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Steven Belledin"
        flavorText = ". . . but the monster within was a tireless hunter."
        imageUri = "https://cards.scryfall.io/normal/back/b/6/b6edac85-78e7-4e90-b538-b67c88bb5c62.jpg?1783940823"
        ruling(
            "2011-01-22",
            "If Hinterland Scourge is attacking, the defending player must assign at least one blocker to it " +
                "during the declare blockers step if that player controls any creatures that could block it.",
        )
    }
}

val HinterlandHermit: CardDefinition =
    CardDefinition.doubleFacedCreature(HinterlandHermitFront, HinterlandScourge)
