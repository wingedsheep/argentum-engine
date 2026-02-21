package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dispersing Orb
 * {3}{U}{U}
 * Enchantment
 * {3}{U}, Sacrifice a permanent: Return target permanent to its owner's hand.
 */
val DispersingOrb = card("Dispersing Orb") {
    manaCost = "{3}{U}{U}"
    typeLine = "Enchantment"
    oracleText = "{3}{U}, Sacrifice a permanent: Return target permanent to its owner's hand."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{U}"),
            Costs.Sacrifice(GameObjectFilter.Permanent)
        )
        val t = target("target", Targets.Permanent)
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Rebecca Guay"
        flavorText = "\"Like the seas, the Æther is fickle and ever-changing. If we can master one, we can control the other.\" —Riptide Project director"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69db0298-f6d5-450f-add3-a28c0a43f33f.jpg?1562919928"
    }
}
