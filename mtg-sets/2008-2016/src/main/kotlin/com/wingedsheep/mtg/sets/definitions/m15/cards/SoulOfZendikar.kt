package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul of Zendikar
 * {4}{G}{G}
 * Creature — Avatar
 * 6/6
 * Reach
 * {3}{G}{G}: Create a 3/3 green Beast creature token.
 * {3}{G}{G}, Exile this card from your graveyard: Create a 3/3 green Beast creature token.
 *
 * Both abilities share one effect; the second is activated from the graveyard with
 * exiling this card as part of its cost.
 */
val SoulOfZendikar = card("Soul of Zendikar") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 6
    oracleText = "Reach\n" +
        "{3}{G}{G}: Create a 3/3 green Beast creature token.\n" +
        "{3}{G}{G}, Exile this card from your graveyard: Create a 3/3 green Beast creature token."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{3}{G}{G}")
        effect = Effects.CreateToken(3, 3, setOf(Color.GREEN), setOf("Beast"))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}{G}"), Costs.ExileSelf)
        effect = Effects.CreateToken(3, 3, setOf(Color.GREEN), setOf("Beast"))
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "201"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/875a2514-3c36-463a-bc0a-67ab4fb2666e.jpg"
    }
}
