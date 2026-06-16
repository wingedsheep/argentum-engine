package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ranger's Firebrand
 * {R}
 * Sorcery
 *
 * Ranger's Firebrand deals 2 damage to any target. The Ring tempts you.
 */
val RangersFirebrand = card("Ranger's Firebrand") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Ranger's Firebrand deals 2 damage to any target. The Ring tempts you."

    spell {
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(2, t).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Pavel Kolomeyets"
        flavorText = "\"Keep close to the fire, with your faces outward! Get some of the longer sticks ready in your hands!\"\n—Strider"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06541200-fa4c-4b98-bdc4-44708fd2ddf6.jpg?1686969118"
    }
}
