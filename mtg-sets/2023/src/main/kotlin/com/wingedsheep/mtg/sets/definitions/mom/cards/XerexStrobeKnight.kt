package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Xerex Strobe-Knight
 * {2}{U}
 * Creature — Human Knight
 * 2/2
 * Flying, vigilance
 * {T}: Create a 2/2 white and blue Knight creature token with vigilance. Activate only if
 * you've cast two or more spells this turn.
 */
val XerexStrobeKnight = card("Xerex Strobe-Knight") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Knight"
    oracleText = "Flying, vigilance\n" +
        "{T}: Create a 2/2 white and blue Knight creature token with vigilance. " +
        "Activate only if you've cast two or more spells this turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.YouCastSpellsThisTurn(atLeast = 2))
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/88439bfc-8942-473b-9e4f-863017788476.jpg?1783916669"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Pavel Kolomeyets"
        flavorText = "Phyrexia broke the laws of reality to invade the planes. " +
            "On Xerex, reality merely bent around the invaders."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e38a32bc-133d-43d1-a6e3-80d39fe53d32.jpg?1783917020"
    }
}
