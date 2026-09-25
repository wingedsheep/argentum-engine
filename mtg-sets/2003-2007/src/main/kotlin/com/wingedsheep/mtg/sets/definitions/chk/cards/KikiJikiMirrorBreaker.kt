package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kiki-Jiki, Mirror Breaker
 * {2}{R}{R}{R}
 * Legendary Creature — Goblin Shaman
 * 2/2
 * Haste
 * {T}: Create a token that's a copy of target nonlegendary creature you control, except it has
 * haste. Sacrifice it at the beginning of the next end step.
 *
 * The Fire Crystal's ability narrowed to nonlegendary targets, with haste added to the copy's
 * copiable values via `addedKeywords` and the delayed sacrifice via `sacrificeAtStep = Step.END`.
 */
val KikiJikiMirrorBreaker = card("Kiki-Jiki, Mirror Breaker") {
    manaCost = "{2}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Shaman"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "{T}: Create a token that's a copy of target nonlegendary creature you control, except it has haste. " +
        "Sacrifice it at the beginning of the next end step."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl().nonlegendary()))
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            addedKeywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
        )
        description = "{T}: Create a token that's a copy of target nonlegendary creature you control, except it has haste. " +
            "Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/162018eb-5483-4fa2-9c5a-abb639eecf91.jpg?1783944301"
        ruling("2021-03-19", "The token copies exactly what was printed on the original creature (except that the copy also has haste) and nothing else. It doesn't copy whether the creature is tapped or untapped, whether it has any counters on it or Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on.")
        ruling("2021-03-19", "If another creature becomes a copy of, or enters the battlefield as a copy of, the token, that creature will copy the creature card the token is copying, except it will also have haste. However, you won't sacrifice the new copy at the beginning of the next end step.")
        ruling("2021-03-19", "If Kiki-Jiki's ability creates multiple tokens due to a replacement effect (such as the one Doubling Season creates), you'll sacrifice each of them.")
    }
}
