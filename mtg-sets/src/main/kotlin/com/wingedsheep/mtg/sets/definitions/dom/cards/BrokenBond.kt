package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Broken Bond
 * {1}{G}
 * Sorcery
 * Destroy target artifact or enchantment. You may put a land card from your hand
 * onto the battlefield.
 */
val BrokenBond = card("Broken Bond") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact or enchantment. You may put a land card from your hand onto the battlefield."

    spell {
        val t = target("target", Targets.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
            .then(HandPatterns.putFromHand(filter = GameObjectFilter.Land))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Ryan Yee"
        flavorText = "\"I can't bear to see another plane broken before I make my own home whole. I'm sorry, but my watch is over.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4dcb59f-2a47-4461-9831-204ad15696b5.jpg?1562741565"
    }
}
