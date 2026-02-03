package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Baleful Stare
 * {2}{U}
 * Sorcery
 * Target opponent reveals their hand. You draw a card for each Mountain and red card in it.
 */
val BalefulStare = card("Baleful Stare") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = CompositeEffect(
            listOf(
                RevealHandEffect(EffectTarget.ContextTarget(0)),
                DrawCardsEffect(
                    count = DynamicAmount.Count(
                        player = Player.TargetOpponent,
                        zone = Zone.Hand,
                        filter = GameObjectFilter.Any.withSubtype("Mountain") or
                            GameObjectFilter.Any.withColor(Color.RED)
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "John Coulthart"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49fb46c8-30ae-4457-a726-6fe1ddd183d5.jpg"
    }
}
