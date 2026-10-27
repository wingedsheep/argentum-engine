package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tuktuk Rubblefort — Zendikar Rising #173
 * {2}{R} · Creature — Wall · 0/3
 *
 * Defender, reach
 * Creatures you control have haste.
 */
val TuktukRubblefort = card("Tuktuk Rubblefort") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wall"
    oracleText = "Defender, reach\nCreatures you control have haste."
    power = 0
    toughness = 3

    keywords(Keyword.DEFENDER, Keyword.REACH)

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Nicholas Gregory"
        flavorText = "As with everything else, goblins believe fortifications benefit from as many spiky bits as possible."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02eedf9d-e26c-405c-aee5-1b3493dc5e9b.jpg"
    }
}
