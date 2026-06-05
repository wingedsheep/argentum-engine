// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByColorEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter


/**
 * Dread Charge
 * {3}{B}
 * Sorcery
 * Black creatures you control can't be blocked this turn except by black creatures.
 */
val DreadCharge = card("Dread Charge") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        effect = GrantCantBeBlockedExceptByColorEffect(filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.BLACK).youControl()), canOnlyBeBlockedByColor = Color.BLACK)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Ted Naifeh"
        flavorText = "\"As equal were their souls, so equal was their fate.\"\n—John Dryden, \"Ode to Mrs. Anne Killigrew\""
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed0ca934-6989-415b-8816-7c66d22b4707.jpg"
    }
}
