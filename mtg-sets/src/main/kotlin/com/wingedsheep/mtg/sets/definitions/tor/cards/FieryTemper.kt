package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fiery Temper (Torment #97)
 * {1}{R}{R}
 * Instant
 *
 * Fiery Temper deals 3 damage to any target.
 * Madness {R}
 *
 * The madness half is entirely the keyword (CR 702.35): discarding this exiles it instead of
 * putting it into the graveyard, and its owner may then cast it for {R}. Cycling a card to discard
 * this one is the classic line — a three-mana Shock becomes a one-mana Lightning Bolt.
 */
val FieryTemper = card("Fiery Temper") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Fiery Temper deals 3 damage to any target.\n" +
        "Madness {R} (If you discard this card, discard it into exile. When you do, cast it for " +
        "its madness cost or put it into your graveyard.)"

    spell {
        target = AnyTarget()
        effect = Effects.DealDamage(3, EffectTarget.ContextTarget(0))
    }

    madness("{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/918e46b7-cbca-4acf-8e83-94b5fcadcc49.jpg?1783945148"
    }
}
