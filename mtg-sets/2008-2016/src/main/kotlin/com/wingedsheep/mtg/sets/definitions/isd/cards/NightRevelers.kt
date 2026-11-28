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
 * Night Revelers
 * {4}{R}
 * Creature — Vampire
 * 4/4
 * This creature has haste as long as an opponent controls a Human.
 */
val NightRevelers = card("Night Revelers") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    oracleText = "This creature has haste as long as an opponent controls a Human."
    power = 4
    toughness = 4
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = Conditions.OpponentControls(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN)),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Steve Argyle"
        flavorText = "Vampires think of themselves as the indisputable nobility of Innistrad, able to do as they please. There are few who can prove them wrong."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3f82c5c-77fa-45f3-a91e-4c2489444855.jpg"
    }
}
