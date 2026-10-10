package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Snap — Urza's Legacy #43 (canonical printing)
 * {1}{U} · Instant
 *
 * Return target creature to its owner's hand. Untap up to two lands.
 *
 * The untap is a resolution-time choice, not targets, and the lands needn't be yours (ruling) —
 * Frantic Search's shape: gather every land on the battlefield, choose up to two, untap them.
 */
val Snap = card("Snap") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. Untap up to two lands."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then
            Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(GameObjectFilter.Land))
                val toUntap = chooseUpTo(2, from = lands)
                run(Effects.TapCollection(collection = toUntap, tap = false))
            }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Mike Raabe"
        flavorText = "Good riddance."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7e0549e-2d23-4ea8-b8d1-ae21af2c9091.jpg?1783946245"
        ruling("2022-12-08", "You choose which lands to untap as the spell resolves. They aren't targeted, and they don't have to be lands that you control.")
    }
}
