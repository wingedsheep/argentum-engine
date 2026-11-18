package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Desecration Plague
 * {3}{G}
 * Sorcery
 * Destroy target enchantment or land.
 */
val DesecrationPlague = card("Desecration Plague") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target enchantment or land."

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.Enchantment or GameObjectFilter.Land))
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Ralph Horsley"
        flavorText = "\"The gods think of us as insects. Perhaps there is wisdom in that.\"\n" +
            "—Perisophia the philosopher"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42dbc5e5-5d46-47f7-a700-b65781a13c7a.jpg?1783939415"
    }
}
