package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Horn of the Mark
 * {2}
 * Legendary Artifact
 *
 * Whenever two or more creatures you control attack a player, look at the top five cards of your
 * library. You may reveal a creature card from among them and put it into your hand. Put the rest
 * on the bottom of your library in a random order.
 */
val HornOfTheMark = card("Horn of the Mark") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact"
    oracleText = "Whenever two or more creatures you control attack a player, look at the top five cards of your library. You may reveal a creature card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer(minAttackers = 2)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature,
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Anastasia Balakchina"
        flavorText = "\"He that blows this horn shall set fear in the hearts of his enemies and joy in the hearts of his friends.\"\n—Éowyn"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd86b71f-d736-426f-bf15-013bc8da1a08.jpg?1686970181"
    }
}
