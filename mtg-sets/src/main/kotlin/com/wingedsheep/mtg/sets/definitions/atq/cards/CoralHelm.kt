package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Coral Helm
 * {3}
 * Artifact
 * {3}, Discard a card at random: Target creature gets +2/+2 until end of turn.
 */
val CoralHelm = card("Coral Helm") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, Discard a card at random: Target creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.DiscardAtRandom(1))
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(2, 2, creature)
        description = "{3}, Discard a card at random: Target creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6df9db-0a46-40a5-ae9d-59f47dae9056.jpg?1562917718"
    }
}
