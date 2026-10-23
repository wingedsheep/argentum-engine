package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Florian, Voldaren Scion
 * {1}{B}{R}
 * Legendary Creature — Vampire Noble
 * 3/3
 * First strike
 * At the beginning of each of your postcombat main phases, look at the top X cards of your
 * library, where X is the total amount of life your opponents lost this turn. Exile one of those
 * cards and put the rest on the bottom of your library in a random order. You may play the exiled
 * card this turn.
 *
 * X is [DynamicAmounts.lifeLostThisTurn] summed over every opponent — life *lost*, never netted
 * against life gained (second ruling). With X = 0 the look is empty and nothing is exiled.
 */
val FlorianVoldarenScion = card("Florian, Voldaren Scion") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Vampire Noble"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "At the beginning of each of your postcombat main phases, look at the top X cards of your " +
        "library, where X is the total amount of life your opponents lost this turn. Exile one of " +
        "those cards and put the rest on the bottom of your library in a random order. You may play " +
        "the exiled card this turn."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(DynamicAmounts.lifeLostThisTurn(Player.EachOpponent))
            )
            val (exiled, rest) = chooseExactlySplit(
                1,
                from = looked,
                prompt = "Exile one card. You may play it this turn.",
                selectedLabel = "Exile (you may play it this turn)",
                remainderLabel = "Put on the bottom of your library",
                showAllCards = true
            )
            exile(exiled)
            toLibraryBottom(rest, order = CardOrder.Random)
            run(Effects.GrantMayPlayFromExile(exiled))
        }
        description = "At the beginning of each of your postcombat main phases, look at the top X " +
            "cards of your library, where X is the total amount of life your opponents lost this " +
            "turn. Exile one of those cards and put the rest on the bottom of your library in a " +
            "random order. You may play the exiled card this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53289e69-74da-46d2-91c2-a11378ba76ef.jpg?1783925560"
        ruling(
            "2021-09-24",
            "If you somehow have more than two main phases in a turn, each main phase after your " +
                "first one is a postcombat main phase, and Florian's last ability triggers at the " +
                "beginning of each of them."
        )
        ruling(
            "2021-09-24",
            "Florian's ability checks only how much life opponents lost during the turn, not by how " +
                "much their life totals decreased compared to the start of the turn. For example, if " +
                "an opponent lost 2 life and then gained 8 life before Florian's ability resolves, " +
                "you'll look at the top two cards of your library."
        )
        ruling(
            "2021-09-24",
            "You must pay all costs and follow all timing rules for cards you play with Florian's " +
                "last ability."
        )
    }
}
