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
 * Thundering Wurm
 * {2}{G}
 * Creature — Wurm
 * 4/4
 * When this creature enters, sacrifice it unless you discard a land card.
 */
val ThunderingWurm = card("Thundering Wurm") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 4
    toughness = 4
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(cost = PayCost.Discard(filter = GameObjectFilter.Land), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b0ba623-d17f-4f0e-b914-da139a3971df.jpg"
    }
}
