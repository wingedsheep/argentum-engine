package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainLifeEffect

/**
 * Sacred Nectar
 * {1}{W}
 * Sorcery
 * You gain 4 life.
 */
val SacredNectar = card("Sacred Nectar") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"

    spell {
        effect = GainLifeEffect(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Janine Johnston"
        flavorText = "\"For he on honey-dew hath fed,\nAnd drunk the milk of Paradise.\"\n—Samuel Taylor Coleridge, \"Kubla Khan\""
        imageUri = "https://cards.scryfall.io/normal/front/4/8/484d1b31-5363-49ef-9b13-2005568636c1.jpg"
    }
}
