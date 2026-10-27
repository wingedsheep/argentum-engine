package com.wingedsheep.mtg.sets.definitions.znr.cards

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
 * Expedition Healer
 * {1}{W}
 * Creature — Kor Cleric
 * 2/2
 *
 * Vigilance
 * This creature has lifelink as long as you control another Cleric.
 *
 * "Another Cleric" is any other permanent with the Cleric subtype (excludeSelf), so it reads the
 * permanent filter rather than creatures only — same shape as Barrow Naughty's "another Faerie".
 */
val ExpeditionHealer = card("Expedition Healer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Cleric"
    power = 2
    toughness = 2
    oracleText = "Vigilance\nThis creature has lifelink as long as you control another Cleric."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.CLERIC),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11a00a88-de0e-4012-9ad8-ccb187dff7df.jpg?1783929419"
    }
}
