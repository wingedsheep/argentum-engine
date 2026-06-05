// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect


/**
 * Mercenary Knight
 * {2}{B}
 * Creature — Human Mercenary Knight
 * 4/4
 * When this creature enters, sacrifice it unless you discard a creature card.
 */
val MercenaryKnight = card("Mercenary Knight") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Mercenary Knight"
    power = 4
    toughness = 4
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(cost = PayCost.Discard(filter = GameObjectFilter.Creature), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9f97a2-b04e-418b-89c7-1c019288f27a.jpg"
    }
}
