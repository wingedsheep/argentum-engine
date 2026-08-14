package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayAdditionalLandsEffect

/**
 * Beorn, Reluctant Host // Till and Tend
 * {4}{G}
 * Legendary Creature — Human Bear Shapeshifter
 * 5/5
 *
 * Trample
 *
 * Adventure: Till and Tend — {1}{G}, Sorcery — Adventure
 * You may play an additional land this turn.
 *
 * The Adventure half is Explore's land-drop grant without the draw, so it reuses
 * [PlayAdditionalLandsEffect] rather than modelling a second way to say the same thing. The grant lasts
 * only for the turn the Adventure resolves, which matters here: the creature half is cast from exile on
 * a later turn and gets no land drop of its own.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster cast
 * it as the creature spell while it remains in exile.)
 */
val BeornReluctantHost = card("Beorn, Reluctant Host") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Bear Shapeshifter"
    power = 5
    toughness = 5
    oracleText = "Trample"

    keywords(Keyword.TRAMPLE)

    adventure("Till and Tend") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "You may play an additional land this turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = PlayAdditionalLandsEffect(count = 1)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Javier Charro"
        flavorText = "He never invited people into his house if he could help it. Now he had got " +
            "fifteen strangers sitting in his porch!"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/804589b7-3ef9-473d-97cc-c61a2d41f70d.jpg?1785323267"
    }
}
