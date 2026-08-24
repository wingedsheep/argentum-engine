package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lumbering Satyr
 * {2}{G}{G}
 * Creature — Satyr Beast
 * 5 / 4
 */
val LumberingSatyr = card("Lumbering Satyr") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Beast"
    oracleText = "All creatures have forestwalk. (They can't be blocked as long as defending player controls a Forest.)"
    power = 5
    toughness = 4

    staticAbility {
        ability = GrantKeyword(
            Keyword.FORESTWALK,
            filter = GroupFilter(GameObjectFilter.Creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Alan Pollack"
        flavorText = "The satyr carves the path that all of Rushwood follows.\n" +
            "—Cho-Arrim saying"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d897088-0667-4864-91c3-5f0ac7f9b220.jpg"
    }
}
