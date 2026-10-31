package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Embereth Skyblazer
 * {3}{R}
 * Creature — Human Knight
 * 4/3
 * During your turn, this creature has flying.
 * Whenever this creature attacks, you may pay {2}{R}. If you do, creatures you control get +X/+0
 * until end of turn, where X is the number of opponents you have.
 *
 * The {2}{R} is a single optional payment on resolution ([Effects.MayPay]); X reads
 * `PlayerCount(EachOpponent)` as the ability resolves, so opponents who already left the game
 * aren't counted (rulings 2019-10-04).
 */
val EmberethSkyblazer = card("Embereth Skyblazer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 3
    oracleText = "During your turn, this creature has flying.\n" +
        "Whenever this creature attacks, you may pay {2}{R}. If you do, creatures you control get +X/+0 " +
        "until end of turn, where X is the number of opponents you have."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{R}"),
            then = Patterns.Group.modifyStatsForAll(
                DynamicAmounts.playerCount(Player.EachOpponent),
                DynamicAmounts.fixed(0),
                GroupFilter.AllCreaturesYouControl,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "321"
        artist = "Daarken"
        flavorText = "\"We fight with valor in our hearts and fire in our veins!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/166ee7fc-a0f7-49af-85bf-d5b36bb2bff4.jpg?1783932551"
        ruling(
            "2019-10-04",
            "While resolving Embereth Skyblazer's triggered ability, you can't pay {2}{R} multiple times to increase the bonus."
        )
        ruling(
            "2019-10-04",
            "The value of X is determined only as Embereth Skyblazer's triggered ability resolves. It doesn't change later in the turn if opponents leave the game."
        )
        ruling(
            "2019-10-04",
            "In a multiplayer game, opponents who have left the game before the ability resolves aren't counted by Embereth Skyblazer's triggered ability."
        )
    }
}
