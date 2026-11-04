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
 * Stampeding Horncrest
 * {4}{R}
 * Creature — Dinosaur
 * 4/4
 * This creature has haste as long as you control another Dinosaur.
 *
 * Same shape as Sun-Crested Pterodon: a [ConditionalStaticAbility] granting haste to the source while
 * you control a *different* Dinosaur (`excludeSelf = true`).
 */
val StampedingHorncrest = card("Stampeding Horncrest") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 4
    oracleText = "This creature has haste as long as you control another Dinosaur."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.DINOSAUR),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Jonathan Kuo"
        flavorText = "When the golden city opened, the dinosaurs inside were eager to greet the new visitors."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d4d4bb3-583c-4c3b-b6b7-342a78bbaec6.jpg?1783935293"
    }
}
