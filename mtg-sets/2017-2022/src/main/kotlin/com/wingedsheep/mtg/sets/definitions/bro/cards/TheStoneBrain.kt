package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * The Stone Brain — The Brothers' War #247
 * {2} · Legendary Artifact
 *
 * {2}, {T}, Exile The Stone Brain: Choose a card name. Search target opponent's graveyard, hand,
 * and library for up to four cards with that name and exile them. That player shuffles, then
 * draws a card for each card exiled from their hand this way. Activate only as a sorcery.
 *
 * Ancient Vendetta's pipeline (free-typed name, any name including basic lands, `chooseUpTo(4)`
 * over a multi-zone search) plus The End's hand-refund tail: the picks still in the hand are
 * filtered off with `currentlyIn(Zone.HAND)` *before* the exile moves them, and that slot's count
 * is how many cards the opponent draws after shuffling. The name is chosen on resolution, so the
 * opponent can't respond knowing it.
 */
val TheStoneBrain = card("The Stone Brain") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{2}, {T}, Exile The Stone Brain: Choose a card name. Search target opponent's " +
        "graveyard, hand, and library for up to four cards with that name and exile them. That " +
        "player shuffles, then draws a card for each card exiled from their hand this way. " +
        "Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.ExileSelf)
        timing = TimingRule.SorcerySpeed
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            // Choose a card name (any name, basic lands included — see the rulings).
            val chosenName = chooseCardName(prompt = "Choose a card name")
            // Search the target opponent's graveyard, hand, and library for cards with that name.
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                search = true
            )
            // Up to four of them — a ceiling, not a requirement.
            val toExile = chooseUpTo(
                4,
                from = matches,
                prompt = "Choose up to four cards with that name to exile",
                selectedLabel = "Exile"
            )
            // Remember which picks came from the hand before the exile moves them.
            val fromHand = filter(toExile, GameObjectFilter.Any.currentlyIn(Zone.HAND))
            exile(toExile, owner = opponent.asPlayer)
            // That player shuffles, then draws a card for each card exiled from their hand.
            run(Effects.ShuffleLibrary(target = opponent))
            run(Effects.DrawCards(fromHand.count, opponent))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3570ebf2-a94c-4621-8808-b06e6e830c06.jpg?1783920013"
        ruling(
            "2022-10-14",
            "You target a player as The Stone Brain's activated ability is put on the stack, but you " +
                "don't name a card until it resolves. Notably, this means that players don't get to " +
                "respond to the ability knowing what card you will name."
        )
        ruling(
            "2022-10-14",
            "Unlike most effects of this kind, The Stone Brain's ability allows its controller to " +
                "choose the name of a basic land. Note that you may only search for up to four copies " +
                "of that card, even if you find more of them."
        )
    }
}
