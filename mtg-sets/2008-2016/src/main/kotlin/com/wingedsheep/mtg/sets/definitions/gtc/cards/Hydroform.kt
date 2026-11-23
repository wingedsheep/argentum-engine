package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hydroform
 * {G}{U}
 * Instant
 *
 * Target land becomes a 3/3 Elemental creature with flying until end of turn. It's still a land.
 *
 * Any land — an opponent's included. The animation adds the creature type with Elemental as its
 * creature subtype, sets base P/T 3/3 and grants flying until end of turn; the land keeps its land
 * types, supertypes and abilities (so it still taps for mana, subject to summoning sickness).
 */
val Hydroform = card("Hydroform") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Target land becomes a 3/3 Elemental creature with flying until end of turn. It's still a land."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.BecomeCreature(
            target = land,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Howard Lyon"
        flavorText = "\"While the sea may be our home, the sky is our destiny.\"\n—Prime Speaker Zegana"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c4e1f41-4aed-451b-bbaa-6cc6780cd6c9.jpg?1783940106"
        ruling("2013-01-24", "If the target land hasn't been under its controller's control continuously since the beginning of their most recent turn, that land won't be able to attack and its {T} abilities won't be able to be activated. In most cases, that means it will no longer be able to be tapped for mana that turn.")
        ruling("2013-01-24", "Hydroform doesn't affect the land's name or any other types, subtypes, or supertypes (such as basic or legendary) the land may have. The land will also keep any abilities it had.")
        ruling("2013-01-24", "Effects that modify power and/or toughness but don't set them to a specific value (like the one created by Giant Growth), power/toughness changes from counters, and effects that switch a creature's power and toughness will continue to apply. This may happen if the land was already a creature when Hydroform resolved.")
    }
}
