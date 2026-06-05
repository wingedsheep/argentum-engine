// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect


/**
 * Pillaging Horde
 * {2}{R}{R}
 * Creature — Human Barbarian
 * 5/5
 * When this creature enters, sacrifice it unless you discard a card at random.
 */
val PillagingHorde = card("Pillaging Horde") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Barbarian"
    power = 5
    toughness = 5
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(cost = PayCost.Discard(random = true), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1daad744-e6b2-4bd8-83df-2e97e9e60d16.jpg"
    }
}
