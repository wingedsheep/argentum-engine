package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Leafkin Druid
 * {1}{G}
 * Creature — Elemental Druid
 * 0/3
 * {T}: Add {G}. If you control four or more creatures, add {G}{G} instead.
 */
val LeafkinDruid = card("Leafkin Druid") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elemental Druid"
    power = 0
    toughness = 3
    oracleText = "{T}: Add {G}. If you control four or more creatures, add {G}{G} instead."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.If(
            condition = Conditions.ControlCreaturesAtLeast(4),
            then = Effects.AddMana(Color.GREEN, 2),
            otherwise = Effects.AddMana(Color.GREEN, 1)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {G}. If you control four or more creatures, add {G}{G} instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Filip Burburan"
        flavorText = "\"I am rooted in the life of all that thrives around me. You cannot move me.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8968a942-72f6-42ad-ae9d-418a1dff0ab3.jpg?1783932963"
    }
}
