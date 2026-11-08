package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Foul Emissary (Eldritch Moon #158)
 * {2}{G}
 * Creature — Human Horror
 * 1/1
 *
 * When this creature enters, look at the top four cards of your library. You may reveal a creature
 * card from among them and put it into your hand. Put the rest on the bottom of your library in any
 * order.
 * When you sacrifice this creature while casting a spell with emerge, create a 3/2 colorless
 * Eldrazi Horror creature token.
 *
 * The second trigger is a SELF leaves-the-battlefield trigger gated on the emerge sacrifice
 * (`Triggers.self.leaves(asEmergeSacrifice = true)`): any other sacrifice, or any other way of
 * leaving, doesn't fire it. No `to` zone — the sacrifice still happened if a replacement effect
 * sends the creature somewhere other than the graveyard.
 */
val FoulEmissary = card("Foul Emissary") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Horror"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal " +
        "a creature card from among them and put it into your hand. Put the rest on the bottom of your " +
        "library in any order.\nWhen you sacrifice this creature while casting a spell with emerge, " +
        "create a 3/2 colorless Eldrazi Horror creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal a creature card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(asEmergeSacrifice = true)
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41446e3b-ecd1-442f-9209-713f0abfc538.jpg"
    }
}
