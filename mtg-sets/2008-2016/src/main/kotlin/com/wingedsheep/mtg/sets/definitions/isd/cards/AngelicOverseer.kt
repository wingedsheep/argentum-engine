package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Angelic Overseer
 * {3}{W}{W}
 * Creature — Angel
 * 5/3
 * Flying
 * As long as you control a Human, this creature has hexproof and indestructible.
 */
val AngelicOverseer = card("Angelic Overseer") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nAs long as you control a Human, this creature has hexproof and indestructible."
    power = 5
    toughness = 3
    keywords(Keyword.FLYING)
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN)),
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN)),
        )
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Jason Chan"
        flavorText = "From mortal hope immortal power springs."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/221d999c-dde1-4a0f-87cf-9e9f44969f94.jpg"
    }
}
