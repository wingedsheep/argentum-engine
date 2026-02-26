package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Skirk Outrider
 * {3}{R}
 * Creature — Goblin
 * 2/2
 * As long as you control a Beast, Skirk Outrider gets +2/+2 and has trample.
 */
val SkirkOutrider = card("Skirk Outrider") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin"
    oracleText = "As long as you control a Beast, this creature gets +2/+2 and has trample."
    power = 2
    toughness = 2

    staticAbility {
        ability = ModifyStats(2, 2, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Beast"))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Beast"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Greg Staples"
        flavorText = "Once the goblins thought about it, they preferred being atop a slateback to being in front of, behind, or underneath one."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/416de0f4-1540-4286-a1ac-4f57301c54e9.jpg?1562908122"
    }
}
