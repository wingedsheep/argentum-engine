package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Serra Ascendant
 * {W}
 * Creature — Human Monk
 * 1/1
 * Lifelink
 * As long as you have 30 or more life, this creature gets +5/+5 and has flying.
 */
val SerraAscendant = card("Serra Ascendant") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 1
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "As long as you have 30 or more life, this creature gets +5/+5 and has flying."

    keywords(Keyword.LIFELINK)

    staticAbility {
        condition = Conditions.LifeAtLeast(30)
        ability = ModifyStats(5, 5, Filters.Self)
    }

    staticAbility {
        condition = Conditions.LifeAtLeast(30)
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee65b44-eeb6-418b-b022-a0aef587c738.jpg?1783941831"
        ruling("2017-11-17", "If Serra Ascendant is dealt damage at the same time that you gain life (most likely because it dealt combat damage to a creature blocking or blocked by it), and your life total becomes 30 or greater, apply its last effect before considering whether or not the damage dealt to it is lethal.")
    }
}
