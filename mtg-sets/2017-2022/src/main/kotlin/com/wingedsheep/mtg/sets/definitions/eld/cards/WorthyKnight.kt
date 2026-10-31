package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Worthy Knight
 * {1}{W}
 * Creature — Human Knight
 * 2/2
 *
 * Whenever you cast a Knight spell, create a 1/1 white Human creature token.
 */
val WorthyKnight = card("Worthy Knight") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Whenever you cast a Knight spell, create a 1/1 white Human creature token."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype(Subtype.KNIGHT))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Yongjae Choi"
        flavorText = "Every Ardenvale aspirant must step through the flame. Their honor determines whether they burn or shine."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b2bde2d-e5df-407e-993c-85880dbb6045.jpg"
    }
}
