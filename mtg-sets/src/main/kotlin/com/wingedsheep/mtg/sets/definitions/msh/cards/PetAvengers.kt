package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pet Avengers — Marvel Super Heroes #178 (common)
 * {3}{G} · Creature — Dragon Cat Dog Bird Frog Hero · 4/4
 *
 * Reach
 * Power-up — {6}{G}: Put a +1/+1 counter on this creature and create a 3/2 white Hero creature
 * token with vigilance. (Activate each power-up ability only once. Reduce the cost by its mana
 * cost if it entered this turn.)
 *
 * Six printed creature types on one common, all preserved verbatim in the type line — they are
 * live text for the set's tribal payoffs, not decoration.
 *
 * `{6}{G}` − `{3}{G}` = `{3}`, and the token is the set's standard 3/2 white Hero with vigilance
 * (shared with Agent Phil Coulson and Hero in Training), so it reuses that token's art.
 */
val PetAvengers = card("Pet Avengers") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon Cat Dog Bird Frog Hero"
    oracleText = "Reach\n" +
        "Power-up — {6}{G}: Put a +1/+1 counter on this creature and create a 3/2 white Hero " +
        "creature token with vigilance. (Activate each power-up ability only once. Reduce the " +
        "cost by its mana cost if it entered this turn.)"
    power = 4
    toughness = 4

    keywords(Keyword.REACH)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{6}{G}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.CreateToken(
                power = 3,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf(Subtype.HERO.value),
                keywords = setOf(Keyword.VIGILANCE),
                imageUri = "https://cards.scryfall.io/normal/front/e/4/e4a64831-eec5-4fc9-8904-19523af3ca42.jpg?1783902804"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Leesha Hannigan"
        flavorText = "\"Lockheed! Hairball! Lockjaw! Redwing! To me!\"\n—Throg"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/108a0b92-2134-4776-a2f7-da92050f1b21.jpg?1783902915"
    }
}
