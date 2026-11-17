package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Reach of Shadows
 * {4}{B}
 * Instant
 * Destroy target creature that's one or more colors.
 *
 * "One or more colors" is [CardPredicate.IsColored], read off projected colors, so a creature
 * made colorless by a continuous effect can't be targeted and one painted a color can.
 */
val ReachOfShadows = card("Reach of Shadows") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature that's one or more colors."

    spell {
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColored)),
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Daarken"
        flavorText = "\"Is it better to be a fool or a coward? Fools may get things done occasionally, but cowards live longer.\"\n—Ghelesh Shadowmane, rakshasa mage"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbf9a803-473a-4c38-b352-d47c4fd93d5e.jpg?1783938693"
    }
}
