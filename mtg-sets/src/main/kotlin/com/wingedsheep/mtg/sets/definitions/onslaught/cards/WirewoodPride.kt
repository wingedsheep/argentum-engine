package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Wirewood Pride
 * {G}
 * Instant
 * Target creature gets +X/+X until end of turn, where X is the number of Elves on the battlefield.
 */
val WirewoodPride = card("Wirewood Pride") {
    manaCost = "{G}"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+X until end of turn, where X is the number of Elves on the battlefield."

    spell {
        target = TargetCreature(filter = TargetFilter.Creature)
        val elfCount = DynamicAmounts.creaturesWithSubtype(Subtype("Elf"))
        effect = Effects.ModifyStats(
            power = elfCount,
            toughness = elfCount,
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "303"
        artist = "Dave Dorman"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/a/5/a559e844-06c9-4953-bc2c-a58e4170fe47.jpg?1562934080"
    }
}
