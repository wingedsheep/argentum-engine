// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect


/**
 * Owl Familiar
 * {1}{U}
 * Creature — Bird
 * 1/1
 * Flying
 * When this creature enters, draw a card, then discard a card.
 */
val OwlFamiliar = card("Owl Familiar") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(
        listOf(
            DrawCardsEffect(1),
            EffectPatterns.discardCards(1)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Janine Johnston"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9587bcb-0ece-4b36-85dc-76899e403b08.jpg"
    }
}
