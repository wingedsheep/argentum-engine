package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainControlEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

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

    spell {
        target = TargetPermanent(filter = TargetFilter.PermanentOpponentControls)
        effect = GainControlEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Greg Staples"
        flavorText = "\"I'll leave subtlety to the rich.\""
        imageUri = "https://cards.scryfall.io/large/front/0/2/0210b3b4-d012-42bc-b1f4-b3571c0f9c5f.jpg?1562895484"
    }
}
