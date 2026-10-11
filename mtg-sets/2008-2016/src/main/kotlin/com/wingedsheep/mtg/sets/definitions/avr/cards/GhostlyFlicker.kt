package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ghostly Flicker
 * {2}{U}
 * Instant
 * Exile two target artifacts, creatures, and/or lands you control, then return those cards to the
 * battlefield under your control.
 *
 * Both targets are exiled together (linked to this spell), then the same batch returns at once
 * under the caster's control — the shape Lake-town Mariners' "Gone Fishing" uses, minus its
 * `underOwnersControl`. If one target becomes illegal, the other is still flickered; a token
 * ceases to exist in exile and never comes back.
 */
val GhostlyFlicker = card("Ghostly Flicker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile two target artifacts, creatures, and/or lands you control, then return those " +
        "cards to the battlefield under your control."

    spell {
        targets(
            TargetFilter(
                (GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land).youControl()
            ),
            count = 2,
        )
        effect = Effects.Pipeline(
            descriptionOverride = "Exile two target artifacts, creatures, and/or lands you control, " +
                "then return those cards to the battlefield under your control"
        ) {
            val chosen = gather(source = CardSource.ChosenTargets)
            exile(chosen, linkToSource = true)
            val returning = gather(source = CardSource.FromLinkedExile())
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Raymond Swanland"
        flavorText = "Sometimes it's useful to take a break from existence."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0a44373-0c50-4e14-a7c6-0de66796b81e.jpg?1783940717"
        ruling("2017-03-14", "The two targets can have different card types. For example, you can target one artifact and one creature with Ghostly Flicker.")
    }
}
