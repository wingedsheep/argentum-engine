package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val ThrashOfRaptors = card("Thrash of Raptors") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "As long as you control another Dinosaur, this creature gets +2/+0 and has trample."

    val anotherDinosaur = Conditions.YouControl(
        GameObjectFilter.Permanent.withSubtype("Dinosaur"),
        excludeSelf = true
    )
    staticAbility {
        ability = ModifyStats(2, 0, GroupFilter.source())
        condition = anotherDinosaur
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source())
        condition = anotherDinosaur
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Jonathan Kuo"
        flavorText = "They glide through the undergrowth, drawn to sounds of disturbance. They attack in unison and all share in the kill. And then they move on."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a03b859-811c-45ff-8d8a-9e87e4bbf113.jpg?1783935735"
    }
}
