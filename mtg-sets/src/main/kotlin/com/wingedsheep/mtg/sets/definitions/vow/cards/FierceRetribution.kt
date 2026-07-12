package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Fierce Retribution
 * {1}{W}
 * Instant
 * Cleave {5}{W} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Destroy target [attacking] creature.
 *
 * Cleave (CR 702.148) is an alternative casting cost that removes the bracketed words when paid.
 * The printed (non-cleaved) cast keeps the brackets, so its target is restricted to an *attacking*
 * creature; paying the cleave cost drops the brackets and broadens the target to any creature.
 *
 * We model that structurally rather than via text mangling: the base [target] carries the
 * bracketed (restricted) requirement and [cleaveTarget] carries the brackets-removed one. The
 * engine swaps in the cleave target requirement when the spell is cast for its cleave cost.
 */
val FierceRetribution = card("Fierce Retribution") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Cleave {5}{W} (You may cast this spell for its cleave cost. If you do, remove " +
        "the words in square brackets.)\nDestroy target [attacking] creature."

    keywordAbility(KeywordAbility.cleave("{5}{W}"))

    spell {
        // Printed (brackets present): destroy target attacking creature.
        val attacker = target("attacking creature", Targets.AttackingCreature)
        effect = Effects.Destroy(attacker)

        // Cleaved (brackets removed): destroy target creature.
        val anyCreature = cleaveTarget("creature", Targets.Creature)
        cleaveEffect = Effects.Destroy(anyCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"It won't bring them back, but it will bring me peace.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9597b163-5c6b-4f64-b1f1-5f1fa2e23e5d.jpg?1782703186"
    }
}
