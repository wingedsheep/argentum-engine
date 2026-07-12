package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bleed Dry
 * {2}{B}{B}
 * Instant
 * Target creature gets -13/-13 until end of turn. If that creature would die this turn, exile it instead.
 */
val BleedDry = card("Bleed Dry") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -13/-13 until end of turn. If that creature would die this turn, exile it instead."
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.ModifyStats(-13, -13, t),
            MarkExileOnDeathEffect(t)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Jodie Muir"
        flavorText = "\"Wipe your chin, child. We can't have Olivia thinking we're uncivilized.\"\n—Anje Falkenrath"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3db65755-f104-4de9-bcc9-0a4a7bc66b51.jpg?1782703123"
    }
}
