package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Smog Elemental
 * {4}{B}{B}
 * Creature — Elemental
 * 3/3
 * Flying
 * Creatures with flying your opponents control get -1/-1.
 */
val SmogElemental = card("Smog Elemental") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elemental"
    oracleText = "Flying\nCreatures with flying your opponents control get -1/-1."
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = -1,
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).opponentControls())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Yeong-Hao Han"
        flavorText = "\"The skies used to be my refuge, a place of peace away from the chaos below. Now, with every flight, death seems to follow me like a shadow.\"\n—Writings of Cica, Skyjek scout"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/667871d3-0d1b-496b-afbd-7504989798e4.jpg"
    }
}
