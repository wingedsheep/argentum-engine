package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dispatch
 * {W}
 * Instant
 *
 * Tap target creature.
 * Metalcraft — If you control three or more artifacts, exile that creature.
 *
 * Metalcraft is checked as the spell resolves, after the creature is tapped.
 */
val Dispatch = card("Dispatch") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Tap target creature.\n" +
        "Metalcraft — If you control three or more artifacts, exile that creature."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t) then Effects.If(
            Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
            Effects.Exile(t)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Erica Yang"
        flavorText = "Venser wondered if it could still be called a teleportation spell if the destination is oblivion."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/496634f9-1271-4be7-bad5-364bb87a6962.jpg?1783941327"
    }
}
