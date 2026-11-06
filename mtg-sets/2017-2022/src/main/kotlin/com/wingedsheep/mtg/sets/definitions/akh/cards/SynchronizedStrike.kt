package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Synchronized Strike
 * {2}{G}
 * Instant
 * Untap up to two target creatures. They each get +2/+2 until end of turn.
 */
val SynchronizedStrike = card("Synchronized Strike") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Untap up to two target creatures. They each get +2/+2 until end of turn."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "David Palumbo"
        flavorText = "Khenra are almost always born as twins. Their instinctual connection lends itself well to their preferred fighting style."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcb09d11-5a6d-498f-9173-3539760b19fb.jpg?1783936466"
    }
}
