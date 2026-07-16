package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity

/**
 * Rural Recruit
 * {3}{G}
 * Creature — Human Peasant
 * 1/1
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * When this creature enters, create a 3/1 green Boar creature token.
 *
 * Two independent pieces:
 *  - [training] gives the keyword + the attack trigger.
 *  - An ETB [Triggers.EntersBattlefield] trigger that makes a 3/1 green Boar token. The Boar's
 *    power (3) exceeds the Recruit's own (1), so the token is exactly the "another creature with
 *    greater power" that lets the Recruit train when they attack together.
 */
val RuralRecruit = card("Rural Recruit") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Peasant"
    power = 1
    toughness = 1
    oracleText = "Training (Whenever this creature attacks with another creature with greater " +
        "power, put a +1/+1 counter on this creature.)\n" +
        "When this creature enters, create a 3/1 green Boar creature token."

    training()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 3,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Boar"),
            imageUri = "https://cards.scryfall.io/normal/front/9/7/975c4329-418a-4ada-82fa-9286fb61d61a.jpg?1783924695"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Eelis Kyttanen"
        flavorText = "The Dawnhart survivors welcome recruits of all shapes and sizes."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bf35971-f1c4-4ccb-af5c-fd396391bb4b.jpg?1783924804"
    }
}
