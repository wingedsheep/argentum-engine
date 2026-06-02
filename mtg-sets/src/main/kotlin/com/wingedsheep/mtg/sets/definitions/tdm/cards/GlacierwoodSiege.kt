package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Glacierwood Siege
 * {1}{G}{U}
 * Enchantment
 *
 * As this enchantment enters, choose Temur or Sultai.
 * • Temur — Whenever you cast an instant or sorcery spell, target player mills four cards.
 * • Sultai — You may play lands from your graveyard.
 *
 * Implementation: the cast-time choice is recorded via [EntersWithChoice] (ChoiceType.MODE).
 * The Temur triggered ability is gated by [SourceChosenModeIs]; the Sultai mode is a
 * [MayPlayLandsFromGraveyard] continuous static ability gated by [SourceChosenModeIs] (which
 * is evaluated during projection so the permission applies only while Sultai is chosen).
 */
val GlacierwoodSiege = card("Glacierwood Siege") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Temur or Sultai.\n" +
        "• Temur — Whenever you cast an instant or sorcery spell, target player mills four cards.\n" +
        "• Sultai — You may play lands from your graveyard."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "temur",
                    label = "Temur",
                    description = "Whenever you cast an instant or sorcery spell, target player mills four cards.",
                    iconKey = "temur"
                ),
                ModeOption(
                    id = "sultai",
                    label = "Sultai",
                    description = "You may play lands from your graveyard.",
                    iconKey = "sultai"
                )
            )
        )
    )

    // Temur — Whenever you cast an instant or sorcery spell, target player mills four cards.
    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        triggerCondition = SourceChosenModeIs("temur")
        val t = target("target", Targets.Player)
        effect = LibraryPatterns.mill(4, t)
    }

    // Sultai — You may play lands from your graveyard.
    staticAbility {
        condition = SourceChosenModeIs("sultai")
        ability = MayPlayLandsFromGraveyard
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f37fad7-2385-409b-8375-fa5dfbcad833.jpg?1743204739"
        ruling("2025-04-04", "If you somehow control Glacierwood Siege and no choice was made for it (perhaps because another permanent on the battlefield became a copy of it), it has neither of the two abilities.")
        ruling("2025-04-04", "Glacierwood Siege's Sultai ability doesn't change the times when you can play those land cards. You can still play only one land per turn, and only during your main phase when you have priority and the stack is empty.")
        ruling("2025-04-04", "Glacierwood Siege's Sultai ability doesn't allow you to activate abilities (such as cycling) of land cards in your graveyard.")
    }
}
