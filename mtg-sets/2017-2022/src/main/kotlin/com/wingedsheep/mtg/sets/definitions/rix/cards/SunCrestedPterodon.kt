package com.wingedsheep.mtg.sets.definitions.rix.cards

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
 * Sun-Crested Pterodon
 * {4}{W}
 * Creature — Dinosaur
 * 2/5
 * Flying
 * This creature has vigilance as long as you control another Dinosaur.
 *
 * Same shape as Shaper Apprentice: a [ConditionalStaticAbility] granting the keyword to the source
 * while you control a *different* permanent with the Dinosaur subtype (`excludeSelf = true`).
 */
val SunCrestedPterodon = card("Sun-Crested Pterodon") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 5
    oracleText = "Flying\nThis creature has vigilance as long as you control another Dinosaur."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.DINOSAUR),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Lars Grant-West"
        flavorText = "\"Pterodons tolerate no intrusion into their skies. Even the clouds must ask permission.\"\n—Mahuiz, Sun Empire archer"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c298b51f-b725-4a51-a91e-4c66cf6dfe9d.jpg"
    }
}
