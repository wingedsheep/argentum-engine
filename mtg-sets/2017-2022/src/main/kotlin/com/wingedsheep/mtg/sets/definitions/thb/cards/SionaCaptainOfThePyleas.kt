package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Siona, Captain of the Pyleas
 * {1}{G}{W}
 * Legendary Creature — Human Soldier
 * 2/2
 * When Siona enters, look at the top seven cards of your library. You may reveal an Aura card from
 * among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 * Whenever an Aura you control becomes attached to a creature you control, create a 1/1 white Human
 * Soldier creature token.
 *
 * The ETB is Ashe's [Patterns.Library.lookAtTopRevealMatchingToHand] over Aura cards. The token
 * trigger is Eriette's "an Aura you control becomes attached" watcher with a creature-you-control
 * host filter, read off projected state at the moment of attaching — so it covers Auras entering
 * attached and Auras moving onto your creatures, and re-attaching to the same host emits nothing
 * (rulings).
 */
val SionaCaptainOfThePyleas = card("Siona, Captain of the Pyleas") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When Siona enters, look at the top seven cards of your library. You may reveal an Aura " +
        "card from among them and put it into your hand. Put the rest on the bottom of your library in a " +
        "random order.\n" +
        "Whenever an Aura you control becomes attached to a creature you control, create a 1/1 white " +
        "Human Soldier creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 7,
            filter = GameObjectFilter.Enchantment.withSubtype("Aura"),
            prompt = "You may reveal an Aura card from among them and put it into your hand",
        )
        description = "When Siona enters, look at the top seven cards of your library. You may reveal an " +
            "Aura card from among them and put it into your hand. Put the rest on the bottom of your " +
            "library in a random order."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.withSubtype("Aura")).becomesAttached(
            to = GameObjectFilter.Creature.youControl(),
            controller = Player.You,
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1e76f0e3-9411-401d-ab38-9c3c64769483.jpg?1783931455",
        )
        description = "Whenever an Aura you control becomes attached to a creature you control, create a " +
            "1/1 white Human Soldier creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b1d890f-7ef8-47b9-9ec8-f3884a1d09db.jpg?1783931518"
        ruling(
            "2020-01-24",
            "Siona's last ability triggers both whenever an Aura enters the battlefield attached to a " +
                "creature you control and whenever an Aura on the battlefield attached to a different object " +
                "becomes attached to a creature you control. It doesn't trigger if an effect tries to attach " +
                "an Aura to the creature it already enchants."
        )
        ruling(
            "2020-01-24",
            "If an Aura becomes attached to Siona that causes it to lose all abilities, Siona's last " +
                "ability won't trigger."
        )
    }
}
