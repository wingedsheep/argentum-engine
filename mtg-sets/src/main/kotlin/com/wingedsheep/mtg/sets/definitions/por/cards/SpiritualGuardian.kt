// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect


/**
 * Spiritual Guardian
 * {3}{W}{W}
 * Creature — Spirit
 * 3/4
 * When this creature enters, you gain 4 life.
 */
val SpiritualGuardian = card("Spiritual Guardian") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 4
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = GainLifeEffect(4)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Terese Nielsen"
        flavorText = "Hope is born within."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbea02f-9124-4e1a-8693-d988a0a3adae.jpg"
    }
}
