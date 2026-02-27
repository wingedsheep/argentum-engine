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
 * Cloudreach Cavalry
 * {1}{W}
 * Creature — Human Soldier
 * 1/1
 * As long as you control a Bird, Cloudreach Cavalry gets +2/+2 and has flying.
 */
val CloudreachCavalry = card("Cloudreach Cavalry") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Soldier"
    oracleText = "As long as you control a Bird, this creature gets +2/+2 and has flying."
    power = 1
    toughness = 1

    staticAbility {
        ability = ModifyStats(2, 2, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Bird"))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Bird"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Kev Walker"
        flavorText = "\"It's easy to lose sight of hope until you gaze out from my vantage point.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65680bda-b999-4c2a-99a8-b03287e00807.jpg?1562915593"
    }
}
