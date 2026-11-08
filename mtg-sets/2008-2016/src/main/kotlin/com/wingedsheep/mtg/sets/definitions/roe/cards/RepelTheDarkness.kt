package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Repel the Darkness — Rise of the Eldrazi #42 (canonical printing)
 * {2}{W} · Instant
 *
 * Tap up to two target creatures.
 * Draw a card.
 *
 * "Up to two target" is one requirement taking zero to two creatures, each tapped in turn; the draw
 * follows unconditionally (Snow Day's shape). With zero targets chosen the spell just draws; if every
 * chosen target is illegal on resolution the spell doesn't resolve and nothing is drawn.
 */
val RepelTheDarkness = card("Repel the Darkness") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures.\nDraw a card."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0))
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Scott Chou"
        flavorText = "A boon to those who cannot see in the dark. A bane to those who live in it."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90fa47c3-3209-4e01-920a-aed2184520c2.jpg?1783942003"
        ruling("2018-12-07", "You may target zero, one, or two creatures. The creatures don't need to be untapped.")
        ruling("2018-12-07", "If you target zero creatures, all that happens is you draw a card.")
        ruling("2018-12-07", "If you target one creature and that target is illegal as Repel the Darkness resolves, the spell doesn't resolve. You don't draw a card.")
        ruling("2018-12-07", "If you target two creatures and they're both illegal as Repel the Darkness resolves, the spell doesn't resolve; you don't draw a card. If just one target is illegal, the spell does resolve; the remaining legal target becomes tapped (if it's untapped at that time) and you draw a card.")
    }
}
