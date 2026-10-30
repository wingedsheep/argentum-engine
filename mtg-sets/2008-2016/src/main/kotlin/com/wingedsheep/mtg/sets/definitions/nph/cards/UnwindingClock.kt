package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps

/**
 * Unwinding Clock
 * {4}
 * Artifact
 * Untap all artifacts you control during each other player's untap step.
 *
 * The Ivorytusk Fortress shape filtered to artifacts. Per the rulings, "doesn't untap during its
 * controller's untap step" effects (Rust Tick, DOESNT_UNTAP) don't stop these untaps — the
 * other-players'-untap-step path doesn't consult that flag.
 */
val UnwindingClock = card("Unwinding Clock") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Untap all artifacts you control during each other player's untap step."

    staticAbility {
        ability = UntapFilteredDuringOtherUntapSteps(filter = GameObjectFilter.Artifact)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Mike Bierek"
        flavorText = "\"The partisans have unearthed a strange artifact. None know its origin, but it gives " +
            "them hope of a life beyond.\"\n—Elspeth's journal"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/495d520b-7560-4ecb-ae62-143eeec5682f.jpg?1783941289"
    }
}
