package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sage's Knowledge
 * {2}{U}
 * Sorcery
 *
 * Return target sorcery card from your graveyard to your hand.
 */
val SagesKnowledge = card("Sage's Knowledge") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target sorcery card from your graveyard to your hand."

    spell {
        val sorcery = target(
            "target",
            TargetObject(
                filter = TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )
        effect = Effects.ReturnToHand(sorcery)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Ding Songjian"
        flavorText = "\"Those who know do not talk. Those who talk do not know.\"\n—Lao Tzu, *Tao Te Ching*\n(trans. Feng and English)"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/156d7c70-6c6d-4052-9d44-029ba1bb66e4.jpg"
    }
}
