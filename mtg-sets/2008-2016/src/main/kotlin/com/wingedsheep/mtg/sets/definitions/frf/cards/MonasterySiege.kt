package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Monastery Siege
 * {2}{U}
 * Enchantment
 *
 * As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of your draw step, draw an additional card, then discard a card.
 * • Dragons — Spells your opponents cast that target you or a permanent you control cost {2} more
 *   to cast.
 *
 * The choice is recorded by [EntersWithChoice] (ChoiceType.MODE); the Khans trigger is gated by
 * [SourceChosenModeIs] and the Dragons tax by [CostGating.OnlyIf] over the same condition.
 */
val MonasterySiege = card("Monastery Siege") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Khans or Dragons.\n" +
        "• Khans — At the beginning of your draw step, draw an additional card, then discard a card.\n" +
        "• Dragons — Spells your opponents cast that target you or a permanent you control cost {2} more to cast."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "khans",
                    label = "Khans",
                    description = "At the beginning of your draw step, draw an additional card, then discard a card.",
                    iconKey = "khans"
                ),
                ModeOption(
                    id = "dragons",
                    label = "Dragons",
                    description = "Spells your opponents cast that target you or a permanent you control cost {2} more.",
                    iconKey = "dragons"
                )
            )
        )
    )

    // Khans — draw an additional card, then discard a card.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        triggerRestriction = SourceChosenModeIs("khans")
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
    }

    // Dragons — a tax on opponents' spells aimed at you or anything you control.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(
                targetFilter = GroupFilter.AllPermanentsYouControl,
                targetPlayer = Player.You,
            ),
            modification = CostModification.IncreaseGeneric(2),
            gating = CostGating.OnlyIf(SourceChosenModeIs("dragons")),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eef398b5-38f7-4a11-8fcf-d4e0d27267cf.jpg?1783938706"
    }
}
