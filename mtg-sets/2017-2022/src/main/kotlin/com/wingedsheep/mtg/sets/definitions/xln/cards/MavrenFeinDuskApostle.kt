package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mavren Fein, Dusk Apostle
 * {2}{W}
 * Legendary Creature — Vampire Cleric
 * 2/2
 * Whenever one or more nontoken Vampires you control attack, create a 1/1 white Vampire creature
 * token with lifelink.
 *
 * "One or more … attack" is the once-per-declaration [Triggers.you.attacks] over a nontoken
 * Vampire attacker filter; the token is created even if those attackers leave combat (ruling).
 */
val MavrenFeinDuskApostle = card("Mavren Fein, Dusk Apostle") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Vampire Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever one or more nontoken Vampires you control attack, create a 1/1 white Vampire " +
        "creature token with lifelink."

    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).nontoken().youControl()
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.LIFELINK),
        )
        description = "Whenever one or more nontoken Vampires you control attack, create a 1/1 white " +
            "Vampire creature token with lifelink."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Daarken"
        flavorText = "In the rapture of his Blood Fast, he destroyed the navigator's tools and steered the ship to shore by faith alone."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7294e40-11b3-4d5c-82d0-c8ec6bd3a6c9.jpg?1783935796"
        ruling("2017-09-29", "Once you attack with one or more nontoken Vampires, Mavren Fein's ability will create a Vampire token even if some or all of those Vampires leave the battlefield or are removed from combat.")
    }
}
