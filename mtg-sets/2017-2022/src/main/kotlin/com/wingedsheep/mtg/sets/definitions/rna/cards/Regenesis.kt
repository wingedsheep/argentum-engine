package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Regenesis — Ravnica Allegiance #136
 * {3}{G}{G} · Instant
 *
 * One optional two-count target over permanent cards you own in your graveyard, each returned
 * to hand by [Effects.ForEachTarget] — the same shape as Rydia's Return's recursion mode.
 */
val Regenesis = card("Regenesis") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Return up to two target permanent cards from your graveyard to your hand."

    spell {
        target = TargetObject(
            optional = true,
            count = 2,
            filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "James Paick"
        flavorText = "\"When you get right down to it, the difference between death and life is just a " +
            "membrane-enclosed environment maintained by a metabolic process.\"\n—Gulistan, Simic biomancer"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18a362c1-af92-4094-a7c2-f09952767606.jpg"
    }
}
