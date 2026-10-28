package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val offerText =
    "Tempting offer — Create X 1/1 red Elemental creature tokens with haste. Each opponent may " +
        "create X 1/1 red Elemental creature tokens with haste. For each opponent who does, create X " +
        "1/1 red Elemental creature tokens with haste."

/**
 * Tempt with Vengeance — Commander 2013 #125
 * {X}{R} · Sorcery
 *
 * Tempting offer — Create X 1/1 red Elemental creature tokens with haste. Each opponent may create
 * X 1/1 red Elemental creature tokens with haste. For each opponent who does, create X 1/1 red
 * Elemental creature tokens with haste.
 *
 * [Patterns.Mechanic.temptingOffer] (the Tempt with Bunnies / Tempt with Discovery shape) with the
 * offer sized by the spell's X. Each accepting opponent runs the offer as their own "you" inside a
 * per-player iteration that keeps the spell's X, so they create X Elementals under their control.
 */
val TemptWithVengeance = card("Tempt with Vengeance") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = offerText

    spell {
        effect = Patterns.Mechanic.temptingOffer(
            offer = Effects.CreateToken(
                count = DynamicAmounts.xValue(),
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Elemental"),
                keywords = setOf(Keyword.HASTE),
                imageUri = "https://cards.scryfall.io/normal/front/6/8/686d9c3d-41dc-476d-93b7-75857b48b1b6.jpg?1783906799"
            ),
            description = offerText,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48ad7c26-edb5-4354-9872-87c667183be7.jpg?1783939666"
        ruling("2013-10-17", "Your opponents decide in turn order whether or not they accept the offer, starting with the opponent on your left. Each opponent will know the decisions of previous opponents in turn order when making their decision.")
        ruling("2013-10-17", "After each opponent has decided, the effect happens simultaneously for each one who accepted the offer. Then, the effect happens again for you a number of times equal to the number of opponents who accepted.")
    }
}
