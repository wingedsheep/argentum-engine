package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captivating Gyre (M20 #51)
 * {4}{U}{U}  Sorcery
 *
 * Return up to three target creatures to their owners' hands.
 */
val CaptivatingGyre = card("Captivating Gyre") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creatures to their owners' hands."

    spell {
        targets(TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Anthony Palumbo"
        flavorText = "\"That's enough of that.\"\n—Atemsis, All-Seeing"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2bd5c34-fa7e-4fa8-a5b1-c3aa928cc834.jpg"
    }
}
