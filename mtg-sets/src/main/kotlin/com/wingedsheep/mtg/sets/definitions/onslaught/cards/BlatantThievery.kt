package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Blatant Thievery
 * {4}{U}{U}{U}
 * Sorcery
 * For each opponent, gain control of target permanent that player controls.
 *
 * In 1v1 this is effectively "Gain control of target permanent an opponent controls."
 */
val BlatantThievery = card("Blatant Thievery") {
    manaCost = "{4}{U}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "For each opponent, gain control of target permanent that player controls."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.PermanentOpponentControls))
        effect = GainControlEffect(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Greg Staples"
        flavorText = "\"I'll leave subtlety to the rich.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8284476c-a7c8-4a6c-8021-ee997e9270ce.jpg?1562925810"
    }
}
