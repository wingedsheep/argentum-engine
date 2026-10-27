package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thraben Watcher — Modern Horizons 2 #34
 * {2}{W}{W} · Creature — Angel · 2/2
 *
 * Flying, vigilance
 * Other nontoken creatures you control get +1/+1 and have vigilance.
 */
private val otherNontokenCreatures = GroupFilter(
    GameObjectFilter.Creature.nontoken().youControl(),
    excludeSelf = true,
)

val ThrabenWatcher = card("Thraben Watcher") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 2
    toughness = 2
    oracleText = "Flying, vigilance\nOther nontoken creatures you control get +1/+1 and have vigilance."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = otherNontokenCreatures)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, otherNontokenCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Bastien L. Deharme"
        flavorText = "\"Fear not the angels' constant eyes,\nE'er on watch from lightless skies;\n" +
            "Eternal vigil will we keep,\nProtectors of the Blessed Sleep.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b4e0e11-2f16-42ad-b835-c9410c4bd7e3.jpg?1783926884"
    }
}
