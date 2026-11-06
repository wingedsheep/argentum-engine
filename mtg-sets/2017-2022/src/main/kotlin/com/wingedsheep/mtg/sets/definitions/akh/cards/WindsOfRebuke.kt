package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Winds of Rebuke
 * {1}{U}
 * Instant
 * Return target nonland permanent to its owner's hand. Each player mills two cards.
 */
val WindsOfRebuke = card("Winds of Rebuke") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand. Each player mills two cards."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.ReturnToHand(permanent) then
            Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Mathias Kollros"
        flavorText = "\"I have foreseen that you will not pass my trial.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2718457-1e3b-48bc-99fe-2300277ee1e0.jpg?1783936513"
    }
}
