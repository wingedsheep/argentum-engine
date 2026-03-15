package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Knight of Grace
 * {1}{W}
 * Creature — Human Knight
 * 2/2
 * First strike
 * Hexproof from black (This creature can't be the target of black spells or abilities
 * your opponents control.)
 * Knight of Grace gets +1/+0 as long as any player controls a black permanent.
 */
val KnightOfGrace = card("Knight of Grace") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Knight"
    oracleText = "First strike\nHexproof from black (This creature can't be the target of black spells or abilities your opponents control.)\nKnight of Grace gets +1/+0 as long as any player controls a black permanent."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.hexproofFrom(Color.BLACK))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, StaticTarget.SourceCreature),
            condition = Exists(
                player = Player.Each,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Any.withColor(Color.BLACK)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"A light to pierce the shadows.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bbbddc0-f8b3-4255-bd82-d50f829ca009.jpg?1584675730"
    }
}
