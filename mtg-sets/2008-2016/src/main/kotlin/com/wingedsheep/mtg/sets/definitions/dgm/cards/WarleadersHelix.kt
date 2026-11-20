package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Warleader's Helix
 * {2}{R}{W}
 * Instant
 * Warleader's Helix deals 4 damage to any target and you gain 4 life.
 */
val WarleadersHelix = card("Warleader's Helix") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Warleader's Helix deals 4 damage to any target and you gain 4 life."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(4, t) then Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Greg Staples"
        flavorText = "\"There is no time to remedy our enemies' ignorance. Blast it out of them.\"\n—Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81e474ac-54f7-43f9-8af9-2f1adf258b15.jpg?1783940018"
    }
}
