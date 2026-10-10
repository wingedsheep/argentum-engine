package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Burgeoning
 * {G}
 * Enchantment
 * Whenever an opponent plays a land, you may put a land card from your hand onto the battlefield.
 *
 * "Plays" is the land-play special action (CR 305.1): a land an effect puts onto the battlefield
 * doesn't trigger it, and the land Burgeoning puts down isn't a land play for its controller.
 */
val Burgeoning = card("Burgeoning") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent plays a land, you may put a land card from your hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.anOpponent.playsLand()
        // The up-to-one selection is the "you may".
        effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Randy Gallegos"
        flavorText = "\"The plants said, 'We will fight the stone with root and stem and seed. We are patient. We will win.'\"\n—Skyshroud myth of the forest"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa9af8e5-bc97-4704-ae5c-e3d2d5b72586.jpg?1783946552"
        ruling("2004-10-04", "Playing a land will trigger it, but putting a land onto the battlefield as part of an effect will not.")
    }
}
