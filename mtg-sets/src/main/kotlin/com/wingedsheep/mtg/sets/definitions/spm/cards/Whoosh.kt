package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Whoosh!
 * {1}{U}
 * Instant
 * Kicker {1}{U}
 * Return target nonland permanent to its owner's hand. If this spell was kicked, draw a card.
 */
val Whoosh = card("Whoosh!") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Kicker {1}{U} (You may pay an additional {1}{U} as you cast this spell.)\n" +
        "Return target nonland permanent to its owner's hand. If this spell was kicked, draw a card."

    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    spell {
        target = Targets.NonlandPermanent
        effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND) then ConditionalEffect(
            condition = WasKicked,
            effect = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Nathaniel Himawan"
        flavorText = "\"Sorry, I have no time for banter today.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccc05deb-ad8d-4fae-a7a4-2b2a118fc696.jpg?1783905348"
    }
}
