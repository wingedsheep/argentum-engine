package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Surrak Dragonclaw
 * {2}{G}{U}{R}
 * Legendary Creature — Human Warrior
 * 6/6
 * Flash
 * This spell can't be countered.
 * Creature spells you control can't be countered.
 * Other creatures you control have trample.
 */
val SurrakDragonclaw = card("Surrak Dragonclaw") {
    manaCost = "{2}{G}{U}{R}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 6
    toughness = 6
    oracleText = "Flash\nThis spell can't be countered.\nCreature spells you control can't be countered.\nOther creatures you control have trample."

    keywords(Keyword.FLASH)

    cantBeCountered = true

    staticAbility {
        ability = GrantCantBeCountered(
            filter = GameObjectFilter.Creature
        )
    }

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter.OtherCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "206"
        artist = "Jaime Jones"
        flavorText = "Both his rank and his scars were earned in single combat against a cave bear."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03032d89-caca-43ff-b2ea-028e376c829c.jpg?1562781878"
        ruling("2014-09-20", "A spell or ability that counters spells can still target a creature spell you control. When that spell or ability resolves, the creature spell won't be countered, but any additional effects of that spell or ability will still happen.")
    }
}
