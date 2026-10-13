package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

val HexbaneTortoise = card("Hexbane Tortoise") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Turtle"
    power = 3
    toughness = 2
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\nEnlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)"

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    staticAbility { ability = Enlist }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acd5a635-bcf5-4a05-b00b-bf40322823fe.jpg?1783921301"
    }
}
