package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Raven Man
 * {1}{B}
 * Legendary Creature — Human Wizard
 * 2/1
 *
 * At the beginning of each end step, if a player discarded a card this turn, create a 1/1 black
 * Bird creature token with flying and "This token can't block."
 * {3}{B}, {T}: Each opponent discards a card. Activate only as a sorcery.
 *
 * - "Each end step" is `Triggers.anyPlayer.beginningOf(Step.END)`. The intervening "if" (CR 603.4)
 *   sums every seat's discard record — `TurnTracking(Player.Each, CARDS_DISCARDED)` — so it fires
 *   once whoever discarded, including The Raven Man's own controller (ruling), and only once
 *   regardless of how many cards were discarded.
 */
val TheRavenMan = card("The Raven Man") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "At the beginning of each end step, if a player discarded a card this turn, create a 1/1 " +
        "black Bird creature token with flying and \"This token can't block.\"\n" +
        "{3}{B}, {T}: Each opponent discards a card. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.cardsDiscardedThisTurn(Player.Each),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            staticAbilities = listOf(CantBlock(GroupFilter.source())),
            imageUri = "https://cards.scryfall.io/normal/front/8/d/8dce95d5-2ae7-4bd9-92f4-1cb3bed5f733.jpg?1783921131",
        )
        description = "At the beginning of each end step, if a player discarded a card this turn, create a 1/1 " +
            "black Bird creature token with flying and \"This token can't block.\""
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.Tap)
        effect = Effects.EachOpponentDiscards(1)
        timing = TimingRule.SorcerySpeed
        description = "Each opponent discards a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e761fb2-ae4f-4d72-a142-f7e9bd681303.jpg?1783921327"
        ruling("2022-09-09", "The Raven Man's first ability triggers at the beginning of each player's end step as long as at least one player has discarded a card this turn. It only triggers once, not once for each card discarded or once for each player that discarded a card. It will trigger even if its controller is the one that discarded a card this turn.")
    }
}
