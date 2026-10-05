package com.wingedsheep.mtg.sets.definitions.xln.cards

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
 * Shaper Apprentice
 * {1}{U}
 * Creature — Merfolk Wizard
 * 2/1
 * This creature has flying as long as you control another Merfolk.
 *
 * The conditional flying is a [ConditionalStaticAbility] granting the keyword to the source only
 * while you control a *different* Merfolk (`excludeSelf = true`). The bare tribal noun "Merfolk"
 * names every *permanent* with the subtype, not just creatures, so the condition filters on
 * [GameObjectFilter.Permanent].
 */
val ShaperApprentice = card("Shaper Apprentice") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "This creature has flying as long as you control another Merfolk."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Yongjae Choi"
        flavorText = "The River Heralds would wreck a thousand ships to keep intruders from finding the golden city."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02955471-5ffc-4c7b-83fe-a69816415ca1.jpg?1783935774"
    }
}
