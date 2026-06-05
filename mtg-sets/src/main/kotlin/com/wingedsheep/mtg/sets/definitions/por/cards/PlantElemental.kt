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
 * Plant Elemental
 * {1}{G}
 * Creature — Plant Elemental
 * 3/4
 * When this creature enters, sacrifice it unless you sacrifice a Forest.
 */
val PlantElemental = card("Plant Elemental") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    power = 3
    toughness = 4
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(cost = PayCost.Sacrifice(GameObjectFilter.Land.withSubtype("Forest")), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Ted Naifeh"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/892594db-1d66-4c45-bd54-608a9972ca77.jpg"
    }
}
