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
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent


/**
 * Ebon Dragon
 * {5}{B}{B}
 * Creature — Dragon
 * 5/4
 * Flying
 * When this creature enters, you may have target opponent discard a card.
 */
val EbonDragon = card("Ebon Dragon") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 4
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetOpponent())
        effect = MayEffect(EffectPatterns.discardCards(1, t))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f10cf69-d3dc-43a4-9595-0f7d245c5efa.jpg"
    }
}
