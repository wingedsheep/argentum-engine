package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Wash Away
 * {U}
 * Instant
 * Cleave {1}{U}{U} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Counter target spell [that wasn't cast from its owner's hand].
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast can only counter spells cast from somewhere *other* than their owner's hand —
 * flashback, foretell, adventure off the stack, cascade, etc.; the cleaved cast counters any
 * spell.
 *
 * We enforce the printed restriction structurally with the reusable
 * `StatePredicate.WasCastFromZone` (via `TargetFilter.notCastFromZone`), which reads the
 * `SpellOnStackComponent.castFromZone` the engine stamps at cast time — no text-only approximation.
 * A spell can only be cast from its own owner's hand (cards in a hand are owned by that hand's
 * player, CR 108.3), so "wasn't cast from its owner's hand" collapses to "wasn't cast from the
 * HAND zone". Paying the cleave cost swaps in the unrestricted spell target.
 */
val WashAway = card("Wash Away") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Cleave {1}{U}{U} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\nCounter target spell [that wasn't cast from its " +
        "owner's hand]."

    keywordAbility(KeywordAbility.cleave("{1}{U}{U}"))

    spell {
        // Printed (brackets present): counter target spell that wasn't cast from its owner's hand.
        target = TargetSpell(filter = TargetFilter.SpellOnStack.notCastFromZone(Zone.HAND))
        effect = Effects.CounterSpell()

        // Cleaved (brackets removed): counter target spell. CounterSpell counters the first
        // chosen target, so an unnamed cleave target requirement is all that's needed.
        cleaveTarget = Targets.Spell
        cleaveEffect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Brian Valeza"
        flavorText = "\"May the ocean take it\" —Nephalia expression meaning \"good riddance\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43411ade-be80-4535-8baa-7055e78496df.jpg?1782731101"
    }
}
