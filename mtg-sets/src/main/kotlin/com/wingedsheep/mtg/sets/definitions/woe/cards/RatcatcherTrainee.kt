package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ratcatcher Trainee // Pest Problem
 * {1}{R}
 * Creature — Human Peasant
 * 2/1
 * During your turn, this creature has first strike.
 *
 * Adventure: Pest Problem — {2}{R}, Instant — Adventure
 * Create two 1/1 black Rat creature tokens with "This token can't block."
 *
 * "During your turn" is a [ConditionalStaticAbility] keyword grant on [Filters.Self] gated by
 * [Conditions.IsYourTurn] — the same shape as Tonberry's Chef's Knife. It is a continuous effect,
 * not a trigger, so first strike appears/disappears as the turn changes (including mid-combat if
 * the turn somehow ends between strike steps).
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val RatcatcherTrainee = card("Ratcatcher Trainee") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Peasant"
    oracleText = "During your turn, this creature has first strike."
    power = 2
    toughness = 1

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    adventure("Pest Problem") {
        manaCost = "{2}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Create two 1/1 black Rat creature tokens with \"This token can't block.\" " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = woeRatToken(DynamicAmount.Fixed(2))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Michele Giorgi"
        flavorText = "\"Where do they keep coming from?!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f4c0959-a107-4d61-9e51-256b2955f6ba.jpg?1783915090"
    }
}
