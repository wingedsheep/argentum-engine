package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Timestream Navigator — Rivals of Ixalan #59
 * {1}{U} · Creature — Human Pirate Wizard · 1/1
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * {2}{U}{U}, {T}, Put this creature on the bottom of its owner's library: Take an extra turn after
 * this one. Activate only if you have the city's blessing.
 *
 * The library move is part of the cost ([Costs.PutSelfOnBottomOfLibrary], Daniels' shape, here from
 * the battlefield — Maze's End's mana + tap + self-move composite), and the blessing gate is
 * Orazca Relic's [ActivationRestriction.OnlyIfCondition].
 */
val TimestreamNavigator = card("Timestream Navigator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate Wizard"
    power = 1
    toughness = 1
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "{2}{U}{U}, {T}, Put this creature on the bottom of its owner's library: Take an extra turn after this one. " +
        "Activate only if you have the city's blessing."

    keywords(Keyword.ASCEND)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}{U}"), Costs.Tap, Costs.PutSelfOnBottomOfLibrary)
        effect = Effects.TakeExtraTurn()
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.YouHaveCitysBlessing))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14770537-209a-4260-88a4-30f4e2b5ede0.jpg"
    }
}
