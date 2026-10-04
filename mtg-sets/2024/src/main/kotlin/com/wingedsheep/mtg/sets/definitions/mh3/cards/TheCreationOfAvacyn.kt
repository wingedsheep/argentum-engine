package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Creation of Avacyn
 * {1}{B}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Search your library for a card, exile it face down, then shuffle.
 * II — Turn the exiled card face up. If it's a creature card, you lose life equal to its mana value.
 * III — You may put the exiled card onto the battlefield if it's a creature card. If you don't put
 *       it onto the battlefield, put it into its owner's hand.
 *
 * - I exiles the found card face down ([FaceDownMode.HIDDEN]) and links it to this Saga
 *   (`linkToSource`), so each Creation of Avacyn owns its own pile — the first ruling.
 * - II and III read that pile back with [CardSource.FromLinkedExile]. II turns every card in it face
 *   up ([Effects.TurnFaceUp] per card) and, if at least one is a creature card, you lose life equal
 *   to the *combined* mana value of the pile — the second ruling.
 * - III: if the pile holds a creature card you may put all of its permanent cards onto the
 *   battlefield (all or none); whatever is still in exile afterwards goes to its owner's hand — the
 *   third ruling.
 */
val TheCreationOfAvacyn = card("The Creation of Avacyn") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Search your library for a card, exile it face down, then shuffle.\n" +
        "II — Turn the exiled card face up. If it's a creature card, you lose life equal to its mana value.\n" +
        "III — You may put the exiled card onto the battlefield if it's a creature card. If you don't " +
        "put it onto the battlefield, put it into its owner's hand."

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.You), search = true)
            val found = chooseExactly(1, from = library, prompt = "Search your library for a card to exile face down")
            exile(found, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
            run(Effects.ShuffleLibrary())
        }
    }

    sagaChapter(2) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            run(Effects.ForEachInCollection(exiled, Effects.TurnFaceUp(EffectTarget.IterationEntity)))
            ifNotEmpty(exiled, filter = GameObjectFilter.Creature) {
                run(Effects.LoseLife(DynamicAmounts.manaValueSumOf(exiled), EffectTarget.Controller))
            }
        }
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            ifNotEmpty(exiled, filter = GameObjectFilter.Creature) {
                val permanents = filter(exiled, GameObjectFilter.Permanent)
                run(
                    Effects.May(
                        Effects.Pipeline { move(permanents, CardDestination.ToZone(Zone.BATTLEFIELD)) },
                        prompt = "Put the exiled card onto the battlefield?"
                    )
                )
            }
            val remaining = filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            toHand(remaining)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27d701aa-df93-4f0e-b1e3-d081487eb456.jpg?1783911283"
        ruling(
            "2024-06-07",
            "Each The Creation of Avacyn you control has its own set of face-down exiled cards. (In most cases, " +
                "this set of cards will contain just one card.) The Creation of Avacyn's second and third chapter " +
                "abilities refer only to those cards, not those of any other The Creation of Avacyn."
        )
        ruling(
            "2024-06-07",
            "In the unusual case where two or more cards are exiled face down with The Creation of Avacyn's first " +
                "chapter ability (likely because the triggered ability was copied or the ability triggered a second " +
                "time), the second chapter ability will turn all of the exiled cards face up. If at least one of them " +
                "is a creature card, you'll lose life equal to the combined mana value of all the exiled cards."
        )
        ruling(
            "2024-06-07",
            "In the unusual case where two or more cards are exiled face down with The Creation of Avacyn's first " +
                "ability when the third chapter ability resolves, if at least one of the exiled cards is a creature " +
                "card, you may choose to put all or none of the exiled cards that are permanent cards onto the " +
                "battlefield. Regardless of what you choose, any remaining exiled cards will be put into their " +
                "owners' hands."
        )
    }
}
