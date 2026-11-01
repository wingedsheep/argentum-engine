package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Azorius Skyguard — Ravnica Allegiance #155
 * {4}{W}{U} · Creature — Human Knight · 3 / 3
 */
val AzoriusSkyguard = card("Azorius Skyguard") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "UW"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Flying, first strike\nCreatures your opponents control get -1/-0."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.opponentControls()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Gabor Szikszai"
        flavorText = "\"These new thopters are all well and good, but four eyes are better than none.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2fddecd-e660-43de-bccc-52f60a089052.jpg?1783933657"
    }
}
