package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Miming Slime
 * {2}{G}
 * Sorcery
 *
 * Create an X/X green Ooze creature token, where X is the greatest power among creatures you control.
 *
 * X is read once, as the token is created on resolution, and becomes the token's base power and
 * toughness — it doesn't change with later changes to your creatures' power (ruling 2018-12-07).
 */
val MimingSlime = card("Miming Slime") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"
    oracleText = "Create an X/X green Ooze creature token, where X is the greatest power among creatures you control."

    spell {
        val greatestPower = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        effect = Effects.CreateDynamicToken(
            dynamicPower = greatestPower,
            dynamicToughness = greatestPower,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            imageUri = "https://cards.scryfall.io/normal/front/d/1/d1821a53-1799-4162-b0b6-f8ab84a55c30.jpg?1783909274",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Svetlin Velinov"
        flavorText = "\"We paid the Simic very well for this capability, but we should quickly recoup our expenses in saved wages.\"\n—Milana, Orzhov prelate"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/128d0584-89d2-499b-b6c2-2425c4ffcd13.jpg?1783940117"
        ruling(
            "2018-12-07",
            "The value of X is the greatest power among creatures you control when Miming Slime resolves. " +
                "If you control no creatures at that time, X will be 0, creating a 0/0 Ooze token that will be " +
                "put into your graveyard as a state-based action (unless something else is raising its toughness)."
        )
        ruling(
            "2018-12-07",
            "The power and toughness of the Ooze token is determined when Miming Slime resolves. It won't " +
                "change as the greatest power among creatures you control changes."
        )
    }
}
