package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Spock, Always Your Friend
 * {U}{R}
 * Legendary Creature — Vulcan Human Scientist
 * 2/2
 *
 * When Spock enters, look at the top four cards of your library. You may reveal a noncreature,
 * nonland card from among them and put it into your hand. Put the rest on the bottom of your
 * library in any order.
 */
val SpockAlwaysYourFriend = card("Spock, Always Your Friend") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Vulcan Human Scientist"
    power = 2
    toughness = 2
    oracleText = "When Spock enters, look at the top four cards of your library. You may reveal a noncreature, nonland card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsNoncreature,
                        CardPredicate.IsNonland
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom"
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.ControllerChooses)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Chris Rallis"
        flavorText = "\"The needs of the many outweigh the needs of the few.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1be731d5-8b21-4883-91c0-dd9070b1eadd.jpg?1792508391"
    }
}
