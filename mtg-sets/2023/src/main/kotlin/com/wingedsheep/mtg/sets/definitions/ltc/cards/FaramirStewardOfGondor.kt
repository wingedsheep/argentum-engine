package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Faramir, Steward of Gondor
 * {1}{W}{U}
 * Legendary Creature — Human Noble
 * 2/2
 * Whenever a legendary creature you control with mana value 4 or greater enters, you become the
 * monarch.
 * At the beginning of your end step, if you're the monarch, create two 1/1 white Human Soldier
 * creature tokens.
 *
 * The end-step clause is an intervening "if" ([interveningIf] on [Conditions.YouAreTheMonarch]):
 * checked when the step begins and again on resolution.
 */
val FaramirStewardOfGondor = card("Faramir, Steward of Gondor") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Noble"
    power = 2
    toughness = 2
    oracleText = "Whenever a legendary creature you control with mana value 4 or greater enters, " +
        "you become the monarch.\n" +
        "At the beginning of your end step, if you're the monarch, create two 1/1 white Human " +
        "Soldier creature tokens."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.legendary().manaValueAtLeast(4).youControl()).enters()
        effect = Effects.BecomeMonarch()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouAreTheMonarch
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/d/b/dbab4309-daca-42d9-ad0b-0457717c4503.jpg?1783916056",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Anna Podedworna"
        flavorText = "\"The last Steward of Gondor begs leave to surrender his office.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c3e8a30-de2a-4d8f-a929-5abeaea2e65a.jpg?1783916020"
        ruling("2023-06-16", "The game starts with no monarch. Once an effect makes one player the monarch, the game will have exactly one monarch from that point forward. As a player becomes the monarch, the current monarch (if any) ceases being the monarch.")
        ruling("2023-06-16", "There are two inherent triggered abilities associated with being the monarch. These triggered abilities have no source and are controlled by the player who was the monarch at the time the abilities triggered. The full texts of these abilities are \"At the beginning of the monarch's end step, that player draws a card\" and \"Whenever a creature deals combat damage to the monarch, its controller becomes the monarch.\"")
    }
}
