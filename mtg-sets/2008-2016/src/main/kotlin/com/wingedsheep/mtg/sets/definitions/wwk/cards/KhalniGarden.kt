package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Khalni Garden
 *
 * Land
 *
 * This land enters tapped.
 * When this land enters, create a 0/1 green Plant creature token.
 * {T}: Add {G}.
 *
 * The gainland shape with the life swapped for a body: an [EntersTapped] replacement effect for
 * the printed first line, a [Triggers.EntersBattlefield] trigger, and a single [Effects.AddMana]
 * ability on [Costs.Tap] (`manaAbility = true` with [TimingRule.ManaAbility], so it resolves
 * without using the stack). The token is the plain [Effects.CreateToken] facade with the printed
 * P/T, colour and creature type — no name or art is baked in here, so the Plant resolves through
 * the set's own `tokenArt` layer.
 */
val KhalniGarden = card("Khalni Garden") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, create a 0/1 green Plant creature token.\n" +
        "{T}: Add {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Plant"),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cc6a5e6-0b73-4488-8954-4b168ce7106d.jpg"
    }
}
