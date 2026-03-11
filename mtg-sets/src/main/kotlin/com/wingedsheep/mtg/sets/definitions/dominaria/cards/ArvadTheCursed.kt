package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Arvad the Cursed
 * {3}{W}{B}
 * Legendary Creature — Vampire Knight
 * 3/3
 * Deathtouch, lifelink
 * Other legendary creatures you control get +2/+2.
 */
val ArvadTheCursed = card("Arvad the Cursed") {
    manaCost = "{3}{W}{B}"
    typeLine = "Legendary Creature — Vampire Knight"
    power = 3
    toughness = 3
    oracleText = "Deathtouch, lifelink\nOther legendary creatures you control get +2/+2."

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(GameObjectFilter.Creature.legendary(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Lius Lasahido"
        flavorText = "\"I won't abandon the Weatherlight. My destiny is to serve at Jhoira's side. This 'illness' means I must trust my faith more and myself less.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e811f37a-f381-42e7-9b4a-15b2241eb10d.jpg?1562744733"
    }
}
