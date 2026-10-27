package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Absorb Identity
 * {1}{U}
 * Instant
 * Return target creature to its owner's hand. You may have Shapeshifters you control become copies
 * of that creature until end of turn.
 *
 * By the time the copy happens the creature is already in its owner's hand, so "that creature" is
 * read as it last existed on the battlefield (CR 608.2h): the bounce records its departure and the
 * copy reads that snapshot's copiable values — a returned Clone is copied as what it was copying.
 * "Shapeshifters" is a bare tribal noun, so it reads *permanent* (as on Shapesharer).
 */
val AbsorbIdentity = card("Absorb Identity") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. You may have Shapeshifters you control become " +
        "copies of that creature until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then Effects.May(
            Effects.EachPermanentBecomesCopyOfTarget(
                target = creature,
                filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Shapeshifter").youControl()),
                duration = Duration.EndOfTurn,
            ),
            prompt = "Have Shapeshifters you control become copies of that creature until end of turn?",
            descriptionOverride = "You may have Shapeshifters you control become copies of that creature until end of turn",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "383"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa1c6bcc-2d43-4ea9-a93f-019793616869.jpg?1783928120"
        ruling(
            "2021-02-05",
            "If the target creature is an illegal target as Absorb Identity tries to resolve, it won't resolve " +
                "and none of its effects will happen. Shapeshifters you control won't become copies of the creature."
        )
        ruling(
            "2021-02-05",
            "Either all Shapeshifters you control will become copies of the creature or none of them will."
        )
        ruling(
            "2021-02-05",
            "If Shapeshifters you control do become copies of the creature, they'll use the copiable values of " +
                "the creature as it last existed on the battlefield. They don't copy whether that creature was " +
                "tapped or untapped, whether it had any counters on it, whether it had any Auras and/or Equipment " +
                "attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on."
        )
        ruling(
            "2021-02-05",
            "If the creature that was returned was copying something else, the Shapeshifters that become copies " +
                "of that creature will use the copiable values of that creature. In most cases, they will be " +
                "copies of whatever that creature was copying. If it was copying a permanent or card with {X} in " +
                "its mana cost, X is 0."
        )
    }
}
