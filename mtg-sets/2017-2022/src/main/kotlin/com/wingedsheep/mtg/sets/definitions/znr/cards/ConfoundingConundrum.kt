package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Confounding Conundrum
 * {1}{U}
 * Enchantment
 * When this enchantment enters, draw a card.
 * Whenever a land an opponent controls enters, if that player had another land enter the
 * battlefield under their control this turn, they return a land they control to its owner's hand.
 *
 * "Had another land enter … this turn" reads the per-player entry log, which already holds the
 * triggering land, so the intervening-if (CR 603.4) is "two or more lands entered under that
 * player's control this turn" — lands that have since left still count, and two lands entering
 * at once with none before each trigger (per the ruling). "That player" is the entering land's
 * controller; they choose any land they control, not necessarily one that entered this turn.
 */
val ConfoundingConundrum = card("Confounding Conundrum") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw a card.\n" +
        "Whenever a land an opponent controls enters, if that player had another land enter the " +
        "battlefield under their control this turn, they return a land they control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.opponentControls()).enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.cardTypeEnteredUnderControlThisTurn(CardType.LAND, Player.ControllerOfTriggeringEntity),
            ComparisonOperator.GTE,
            2
        )
        effect = Effects.ForEachPlayer(
            Player.ControllerOfTriggeringEntity,
            Effects.Pipeline {
                val lands = gather(
                    CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Land,
                        player = Player.You,
                    ),
                    name = "confoundingConundrumLands",
                )
                val bounced = chooseExactly(
                    1,
                    from = lands,
                    prompt = "Return a land you control to its owner's hand",
                    useTargetingUI = true,
                )
                toHand(bounced)
            },
        )
        description = "Whenever a land an opponent controls enters, if that player had another land " +
            "enter the battlefield under their control this turn, they return a land they control to " +
            "its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95e37936-913e-4b16-a5a8-8aed733d702d.jpg?1783929399"
        ruling("2020-09-25", "The player may return any land they control to its owner's hand, not just one of those that entered this turn.")
        ruling("2020-09-25", "If an opponent has had no lands enter under their control and then two or more lands enter at the same time, Confounding Conundrum's second ability triggers for each of those lands.")
    }
}
