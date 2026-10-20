package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Glyph of Life — Legends #15
 * {W} · Instant
 *
 * Choose target Wall creature. Whenever that creature is dealt damage by an attacking creature this
 * turn, you gain that much life.
 *
 * An event-based delayed trigger (expires at end of turn) whose `watchedRecipient` bakes in the
 * Wall, so only damage dealt *to* that Wall counts, and whose source filter demands an attacking
 * creature — combat or noncombat damage alike. The caster controls the delayed trigger, so "you"
 * is the Glyph's caster even when the Wall is an opponent's; "that much" reads the damage event.
 */
val GlyphOfLife = card("Glyph of Life") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose target Wall creature. Whenever that creature is dealt damage by an attacking " +
        "creature this turn, you gain that much life."

    spell {
        val wall = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Wall")))
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.attacking()).dealsDamage(),
            watchedRecipient = wall,
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Susan Van Camp"
        flavorText = "Any wall can be battered down, but at what cost?"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba1384e5-d140-4074-9548-250af09cb413.jpg"
    }
}
