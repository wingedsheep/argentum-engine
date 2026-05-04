package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Biomechan Engineer
 * {G}{U}
 * Creature — Insect Artificer
 * When this creature enters, create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 * {8}: Draw two cards and create a 2/2 colorless Robot artifact creature token.
 * 2/2
 */
val BiomechanEngineer = card("Biomechan Engineer") {
    manaCost = "{G}{U}"
    typeLine = "Creature — Insect Artificer"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")\n{8}: Draw two cards and create a 2/2 colorless Robot artifact creature token."

    // ETB: create a Lander token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateLander()
    }

    // Activated ability: {8}: Draw two cards and create a 2/2 Robot token
    activatedAbility {
        cost = Costs.Mana("{8}")
        effect = CompositeEffect(
            listOf(
                Effects.DrawCards(2),
                CreateTokenEffect(
                    power = 2,
                    toughness = 2,
                    colors = setOf(),
                    creatureTypes = setOf("Robot"),
                    artifactToken = true,
                    imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Monztre"
        flavorText = "\"Life is a pattern, not a material.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7edcef2c-029c-46bd-bfeb-ff56a57dd63a.jpg?1752947428"
    }
}
