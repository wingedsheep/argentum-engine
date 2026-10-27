package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kor Blademaster — Zendikar Rising #21
 * {1}{W} · Creature — Kor Warrior · 1/1
 *
 * Double strike
 * Equipped Warriors you control have double strike.
 */
val KorBlademaster = card("Kor Blademaster") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Warrior"
    power = 1
    toughness = 1
    oracleText = "Double strike\nEquipped Warriors you control have double strike."

    keywords(Keyword.DOUBLE_STRIKE)

    // Equipped Warriors you control have double strike.
    staticAbility {
        ability = GrantKeyword(
            Keyword.DOUBLE_STRIKE,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Warrior").youControl().equipped()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Darren Tan"
        flavorText = "Every kor blade is imbued with the skill of a thousand generations."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc6d6049-5599-47eb-ae58-a8eb96927ece.jpg?1783929416"

        ruling(
            "2020-09-25",
            "If a creature loses double strike after assigning damage in the first strike combat " +
                "damage step (due to Kor Blademaster leaving the battlefield, for example), that " +
                "creature won't assign damage in the normal combat damage step."
        )
    }
}
