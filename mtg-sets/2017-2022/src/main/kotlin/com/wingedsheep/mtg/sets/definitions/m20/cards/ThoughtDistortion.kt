package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Thought Distortion (M20 #117)
 * {4}{B}{B}  Sorcery
 *
 * This spell can't be countered.
 * Target opponent reveals their hand. Exile all noncreature, nonland cards from that player's
 * hand and graveyard.
 *
 * Amnesia's reveal → gather → move pipeline, run over both the hand and the graveyard, exiling
 * instead of discarding.
 */
val ThoughtDistortion = card("Thought Distortion") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell can't be countered.\n" +
        "Target opponent reveals their hand. Exile all noncreature, nonland cards from that player's hand and graveyard."

    cantBeCountered = true

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val noncreatureNonland = GameObjectFilter(
                cardPredicates = listOf(CardPredicate.IsNoncreature, CardPredicate.IsNonland)
            )
            val fromHand = gather(
                CardSource.FromZone(zone = Zone.HAND, player = opponent.asPlayer, filter = noncreatureNonland)
            )
            exile(fromHand, owner = opponent.asPlayer)
            val fromGraveyard = gather(
                CardSource.FromZone(zone = Zone.GRAVEYARD, player = opponent.asPlayer, filter = noncreatureNonland)
            )
            exile(fromGraveyard, owner = opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "David Palumbo"
        flavorText = "\"The skull is rigid, but the mind is so deliciously supple.\"\n—Eromena the Mind Render"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4a9d636-6720-4d7e-8b81-12c20bce6495.jpg?1783932987"
    }
}
