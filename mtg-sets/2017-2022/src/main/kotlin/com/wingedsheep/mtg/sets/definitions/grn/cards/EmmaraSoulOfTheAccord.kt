package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Emmara, Soul of the Accord
 * {G}{W}
 * Legendary Creature — Elf Cleric
 * 2/2
 * Whenever Emmara becomes tapped, create a 1/1 white Soldier creature token with lifelink.
 */
val EmmaraSoulOfTheAccord = card("Emmara, Soul of the Accord") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Cleric"
    oracleText = "Whenever Emmara becomes tapped, create a 1/1 white Soldier creature token with lifelink."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.LIFELINK)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Mark Winters"
        flavorText = "\"Whatever hatred destroys, a single act of trust can revive.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41b930ee-e16b-4612-87de-c03ecc6ff6db.jpg?1783934136"
    }
}
