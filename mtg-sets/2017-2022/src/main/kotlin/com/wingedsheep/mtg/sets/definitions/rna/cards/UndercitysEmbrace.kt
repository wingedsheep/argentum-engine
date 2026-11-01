package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Undercity's Embrace — Ravnica Allegiance #89
 * {2}{B} · Instant
 *
 * The life gain checks on resolution, after the sacrifice.
 */
val UndercitysEmbrace = card("Undercity's Embrace") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target opponent sacrifices a creature of their choice. " +
        "If you control a creature with power 4 or greater, you gain 4 life."

    spell {
        val t = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, t) then
            Effects.If(
                condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
                then = Effects.GainLife(4),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Tyler Walpole"
        flavorText = "The undercity is always hungry."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfcb9805-eca0-476c-9480-0c958acdb50f.jpg?1783933687"
    }
}
