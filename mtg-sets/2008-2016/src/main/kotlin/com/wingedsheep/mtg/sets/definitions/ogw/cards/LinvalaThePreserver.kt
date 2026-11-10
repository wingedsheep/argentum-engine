package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Linvala, the Preserver
 * {4}{W}{W}
 * Legendary Creature — Angel
 * 5/5
 *
 * Flying
 * When Linvala enters, if an opponent has more life than you, you gain 5 life.
 * When Linvala enters, if an opponent controls more creatures than you, create a 3/3 white
 * Angel creature token with flying.
 *
 * Both are intervening-if triggers (CR 603.4), checked as Linvala enters and again on
 * resolution. "An opponent" is existential over single opponents, so the life check compares
 * the greatest opponent life total against yours (never one arbitrary opponent's).
 */
val LinvalaThePreserver = card("Linvala, the Preserver") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Linvala enters, if an opponent has more life than you, you gain 5 life.\n" +
        "When Linvala enters, if an opponent controls more creatures than you, create a 3/3 white Angel creature token with flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.greatestAmongPlayers(DynamicAmounts.lifeTotal(Player.You), Player.EachOpponent),
            ComparisonOperator.GT,
            DynamicAmounts.lifeTotal(Player.You)
        )
        effect = Effects.GainLife(5)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentControlsMoreCreatures
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/9/6/96f407ef-2b08-45e9-a45e-128cf0a63839.jpg?1783937862"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "25"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef0dbb08-d124-46da-bfb1-af7ef4a18179.jpg?1783937925"
        ruling(
            "2016-01-22",
            "For each triggered ability, the game checks to see if the condition is true at the moment the ability would trigger. If the condition isn't true, the ability won't trigger. If the ability does trigger, the game will check again to see if the condition is true as the ability tries to resolve. If it's not true at that time, the ability won't do anything."
        )
        ruling(
            "2016-01-22",
            "In a multiplayer game, it's possible for the condition to be true at both times but with respect to two different opponents."
        )
    }
}
