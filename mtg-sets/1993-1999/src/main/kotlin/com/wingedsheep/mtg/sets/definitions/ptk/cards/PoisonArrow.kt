package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Poison Arrow
 * {4}{B}{B}
 * Sorcery
 * Destroy target nonblack creature. You gain 3 life.
 */
val PoisonArrow = card("Poison Arrow") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target nonblack creature. You gain 3 life."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = Effects.Composite(
            Effects.Destroy(t),
            Effects.GainLife(3)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Li Tie"
        flavorText = "In ancient China, not wearing armor could be a fatal mistake. Guan Yu, Zhou Yu, Sun Ce, and other heros were struck by poison arrows."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b7b5f34-c250-484e-9bae-94789b2a87fb.jpg"
    }
}
