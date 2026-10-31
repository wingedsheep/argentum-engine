package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Silverwing Squadron
 * {5}{W}
 * Creature — Human Knight
 * * / *
 * Flying, vigilance
 * Silverwing Squadron's power and toughness are each equal to the number of creatures you control.
 * Whenever this creature attacks, create a number of 2/2 white Knight creature tokens with vigilance
 * equal to the number of opponents you have.
 *
 * The token count reads `PlayerCount(EachOpponent)` at resolution, which drops opponents who have
 * left the game — matching the 2019-10-04 ruling.
 */
val SilverwingSquadron = card("Silverwing Squadron") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Flying, vigilance\n" +
        "Silverwing Squadron's power and toughness are each equal to the number of creatures you control.\n" +
        "Whenever this creature attacks, create a number of 2/2 white Knight creature tokens with vigilance " +
        "equal to the number of opponents you have."
    power = 0
    toughness = 0
    dynamicStats(DynamicAmounts.creaturesYouControl())

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = DynamicAmounts.playerCount(Player.EachOpponent),
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/7/0/703e7ecf-3d73-40c1-8cfe-0758778817cf.jpg?1783932483",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "315"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4348513e-4a03-4e06-98e0-ac6c0dfb013d.jpg?1783932552"
        ruling(
            "2019-10-04",
            "The ability that defines Silverwing Squadron's power and toughness works in all zones, not just the battlefield."
        )
        ruling(
            "2019-10-04",
            "As long as Silverwing Squadron is on the battlefield, its ability will count itself, so it'll be at least 1/1."
        )
        ruling(
            "2019-10-04",
            "In a multiplayer game, opponents who have left the game aren't counted by Silverwing Squadron's triggered ability."
        )
    }
}
