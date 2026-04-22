package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Mirrorform
 * {4}{U}{U}
 * Instant
 *
 * Each nonland permanent you control becomes a copy of target non-Aura permanent.
 */
val Mirrorform = card("Mirrorform") {
    manaCost = "{4}{U}{U}"
    typeLine = "Instant"
    oracleText = "Each nonland permanent you control becomes a copy of target non-Aura permanent."

    spell {
        val permanent = target(
            "non-Aura permanent",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.Permanent.notSubtype(Subtype.AURA))
            )
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(target = permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Wayne Reynolds"
        flavorText = "If you stare long enough at your reflection, your reflection starts staring back."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55d30256-f5d8-4f61-a3f5-878970ced6d1.jpg?1767659560"
        ruling("2025-11-17", "If the copied permanent has {X} in its mana cost, X is 0.")
        ruling("2025-11-17", "If the copied permanent is copying something else, then the permanents become copies of whatever that permanent copied.")
        ruling("2025-11-17", "Because the permanents aren't entering the battlefield when they become copies of the target permanent, any \"When [this permanent] enters\" or \"[This permanent] enters with\" abilities of the copied permanent won't apply.")
        ruling("2025-11-17", "The permanents copy exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else). They don't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
    }
}
