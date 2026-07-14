package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Alchemist's Retrieval
 * {U}
 * Instant
 * Cleave {1}{U} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Return target nonland permanent [you control] to its owner's hand.
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast can only bounce a nonland permanent *you control* (a defensive save from removal);
 * the cleaved cast bounces any nonland permanent (a tempo play against an opponent).
 *
 * Target-only difference: the base [target] carries the "you control" restriction and
 * [cleaveTarget] drops it. Both modes return the chosen target to its owner's hand — the effect is
 * identical, so only the target requirement is swapped when cast for the cleave cost.
 */
val AlchemistsRetrieval = card("Alchemist's Retrieval") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Cleave {1}{U} (You may cast this spell for its cleave cost. If you do, remove " +
        "the words in square brackets.)\nReturn target nonland permanent [you control] to its " +
        "owner's hand."

    keywordAbility(KeywordAbility.cleave("{1}{U}"))

    spell {
        // Printed (brackets present): return target nonland permanent you control.
        val owned = target(
            "nonland permanent you control",
            TargetPermanent(filter = TargetFilter.NonlandPermanent.youControl()),
        )
        effect = Effects.ReturnToHand(owned)

        // Cleaved (brackets removed): return target nonland permanent.
        val any = cleaveTarget("nonland permanent", Targets.NonlandPermanent)
        cleaveEffect = Effects.ReturnToHand(any)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "David Auden Nash"
        flavorText = "To most, a terrifying apparition. To a necro-alchemist, a potent fuel source."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edbf9d4f-6027-40b8-81c1-7f001a9119dd.jpg?1783924901"
    }
}
