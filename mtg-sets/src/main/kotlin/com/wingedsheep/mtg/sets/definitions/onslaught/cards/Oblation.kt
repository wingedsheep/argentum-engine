package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oblation
 * {2}{W}
 * Instant
 * The owner of target nonland permanent shuffles it into their library, then draws two cards.
 */
val Oblation = card("Oblation") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "The owner of target nonland permanent shuffles it into their library, then draws two cards."

    spell {
        target = Targets.NonlandPermanent
        effect = Effects.ShuffleIntoLibrary(EffectTarget.ContextTarget(0)) then
                DrawCardsEffect(2, EffectTarget.PlayerRef(Player.OwnerOf("target permanent")))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Doug Chaffee"
        flavorText = "\"A richer people could give more but they could never give as much.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58561356-4a97-467b-88e5-412e633715fb.jpg?1562915764"
    }
}
