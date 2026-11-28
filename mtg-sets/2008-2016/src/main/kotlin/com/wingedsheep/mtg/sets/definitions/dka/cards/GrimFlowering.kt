package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grim Flowering — Dark Ascension #117 (canonical printing)
 * {5}{G}
 * Sorcery
 * Draw a card for each creature card in your graveyard.
 *
 * The count is read on resolution; Grim Flowering itself is still on the stack (and isn't a
 * creature card), so it never counts itself.
 */
val GrimFlowering = card("Grim Flowering") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Draw a card for each creature card in your graveyard."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.creatureCardsInYourGraveyard())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Adam Paquette"
        flavorText = "\"Nothing in nature goes to waste, not even the rotting corpse of a good-for-nothing, " +
            "blood-sucking vampire.\"\n—Halana of Ulvenwald"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5f3e2ad-7a04-4735-ba73-576e32249ba3.jpg?1783940807"
    }
}
