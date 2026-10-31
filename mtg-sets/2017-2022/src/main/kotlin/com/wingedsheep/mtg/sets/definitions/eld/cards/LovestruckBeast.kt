package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lovestruck Beast // Heart's Desire
 * {2}{G}
 * Creature — Beast Noble
 * 5/5
 * This creature can't attack unless you control a 1/1 creature.
 *
 * Adventure: Heart's Desire — {G}, Sorcery — Adventure
 * Create a 1/1 white Human creature token.
 *
 * The 1/1 check reads projected power/toughness and does not exclude the Beast itself (no "another"),
 * so a Beast shrunk to 1/1 satisfies its own restriction.
 */
val LovestruckBeast = card("Lovestruck Beast") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast Noble"
    power = 5
    toughness = 5
    oracleText = "This creature can't attack unless you control a 1/1 creature."

    staticAbility {
        ability = CantAttackUnless(
            Conditions.YouControl(GameObjectFilter.Creature.power(1).toughnessAtLeast(1).toughnessAtMost(1))
        )
    }

    adventure("Heart's Desire") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create a 1/1 white Human creature token. (Then exile this card. You may cast the " +
            "creature later from exile.)"
        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Human")
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Kev Walker"
        flavorText = "His mind chose solitude, but his heart disagreed."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ccdef9c-1e85-4358-8059-8972479f7556.jpg?1783932607"
    }
}
