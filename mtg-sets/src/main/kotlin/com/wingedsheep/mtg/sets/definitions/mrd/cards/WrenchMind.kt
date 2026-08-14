package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Wrench Mind — Mirrodin #84
 * {B}{B} · Sorcery
 *
 * Target player discards two cards unless they discard an artifact card.
 *
 * "Unless" here is a *choice made by the discarding player as the spell resolves*, not a cost and
 * not a condition checked beforehand: they either pitch two cards of their choosing, or a single
 * artifact card. [Patterns.Hand.discardCardsUnlessMatching] is exactly that shape — a
 * choose-exactly-two selection over the target player's hand carrying a
 * `ReducedMinimumIfMatches` restriction that drops the minimum to one as soon as the selection
 * holds an artifact card.
 *
 * `EffectTarget.ContextTarget(0)` scopes both the gathered hand and the chooser to the targeted
 * player, so they pick from their own cards. A player with fewer than two cards in hand discards
 * what they have; an empty hand makes it a no-op, and the spell still resolves (a player is a
 * legal target regardless of hand size).
 */
val WrenchMind = card("Wrench Mind") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards unless they discard an artifact card."

    spell {
        target = TargetPlayer()
        effect = Patterns.Hand.discardCardsUnlessMatching(
            count = 2,
            unlessFilter = GameObjectFilter.Artifact,
            target = EffectTarget.ContextTarget(0),
            prompt = "Discard two cards, or a single artifact card"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Pete Venters"
        flavorText = "What is the sound of one head snapping?"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36ce975b-c3df-4472-a6c1-2546df11b74e.jpg?1783944542"
    }
}
