package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Beseech the Mirror
 * {1}{B}{B}{B}
 * Sorcery
 *
 * Bargain
 * Search your library for a card, exile it face down, then shuffle. If this spell was
 * bargained, you may cast the exiled card for free if its mana value is 4 or less. Put it
 * into your hand if it wasn't cast this way.
 */
val BeseechTheMirror = card("Beseech the Mirror") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)\n" +
        "Search your library for a card, exile it face down, then shuffle. If this spell was " +
        "bargained, you may cast the exiled card without paying its mana cost if that spell's " +
        "mana value is 4 or less. Put the exiled card into your hand if it wasn't cast this way."

    bargain()

    spell {
        effect = Effects.Pipeline {
            val beseechLibrary = gather(CardSource.FromZone(Zone.LIBRARY, Player.You), search = true)
            val beseechFound = chooseExactly(1, from = beseechLibrary, prompt = "Search your library for a card")
            val beseechExiled = moveTracked(
                beseechFound,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN
            )
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(Effects.If(
                condition = Conditions.WasBargained,
                then = Effects.Pipeline {
                    val beseechCastable = filter(
                        beseechExiled,
                        GameObjectFilter.Any.manaValueAtMostDynamic(DynamicAmounts.fixed(4))
                    )
                    ifNotEmpty(beseechCastable) {
                        run(Effects.May(
                            Effects.CastFromCollectionWithoutPayingCost(beseechCastable)
                        ))
                    }
                }
            ))
            val beseechUncast = filter(beseechExiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            toHand(beseechUncast)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "82"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18c59776-e1f1-4197-a128-db1d603f56b7.jpg?1783915111"

        ruling("2023-09-01", "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost.")
        ruling("2023-09-01", "If you copy a bargained spell, the copy is also bargained.")
    }
}
