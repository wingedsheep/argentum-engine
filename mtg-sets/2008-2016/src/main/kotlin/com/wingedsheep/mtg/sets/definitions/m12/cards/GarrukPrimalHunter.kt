package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Garruk, Primal Hunter — Magic 2012 #174 (canonical printing; reprinted in M13, C19, C21)
 * {2}{G}{G}{G} · Legendary Planeswalker — Garruk · Loyalty 3
 *
 * +1: Create a 3/3 green Beast creature token.
 * −3: Draw cards equal to the greatest power among creatures you control.
 * −6: Create a 6/6 green Wurm creature token for each land you control.
 *
 * The −3 draws the MAX-power aggregate of your creatures (as Fungal Sprouting counts it); the −6
 * counts [DynamicAmounts.landsYouControl] (as Avenger of Zendikar does). Both amounts are read on
 * resolution, per the rulings; a greatest power of 0 or less draws nothing.
 */
val GarrukPrimalHunter = card("Garruk, Primal Hunter") {
    manaCost = "{2}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 3
    oracleText = "+1: Create a 3/3 green Beast creature token.\n" +
        "−3: Draw cards equal to the greatest power among creatures you control.\n" +
        "−6: Create a 6/6 green Wurm creature token for each land you control."

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/3/b/3bb48d4f-e39f-4d51-9bb1-efca7dbfdc83.jpg?1783941037"
        )
    }

    loyaltyAbility(-3) {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        )
    }

    loyaltyAbility(-6) {
        effect = Effects.CreateToken(
            count = DynamicAmounts.landsYouControl(),
            power = 6,
            toughness = 6,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wurm"),
            imageUri = "https://cards.scryfall.io/normal/front/8/4/84dc847c-7a37-4c7f-b02c-30b3e4c91fb6.jpg?1783941037"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82d586bf-bed3-4390-b81d-d101c2ae524c.jpg?1783941060"

        ruling(
            "2011-09-22",
            "For Garruk's second ability, the greatest power among creatures you control is determined " +
                "when that ability begins resolving."
        )
        ruling(
            "2011-09-22",
            "If the greatest power among creatures you control is 0 or less, you'll draw no cards. " +
                "You won't discard any cards this way."
        )
        ruling(
            "2011-09-22",
            "For Garruk's third ability, the number of Wurm tokens you put onto the battlefield is " +
                "determined when that ability begins resolving."
        )
    }
}
