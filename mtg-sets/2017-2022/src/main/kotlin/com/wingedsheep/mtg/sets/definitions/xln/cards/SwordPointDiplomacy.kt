package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sword-Point Diplomacy — Ixalan #126
 * {2}{B} · Sorcery
 *
 * Reveal the top three cards of your library. For each of those cards, put that card into your
 * hand unless any opponent pays 3 life. Then exile the rest.
 *
 * All three cards are revealed first (ruling). Then the caster picks the order the cards are put to
 * the opponents (ruling: "You choose the order to perform this process for the cards") — a
 * `chooseExactlySplit(1)` off the revealed pile, twice, the last card forced. Each card in turn is
 * re-revealed so the opponents can see which card is at stake, then
 * [Effects.UnlessAnyPlayerPays] restricted to [Player.EachOpponent] asks each opponent in turn
 * order whether to pay 3 life; the first to pay stops the card going to hand, and if none does it
 * moves to its owner's hand. Every card still in the library afterwards — the ones an opponent paid
 * for — is exiled together ("then exile the rest").
 */
val SwordPointDiplomacy = card("Sword-Point Diplomacy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Reveal the top three cards of your library. For each of those cards, put that " +
        "card into your hand unless any opponent pays 3 life. Then exile the rest."

    spell {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(3), revealed = true)

            // "Put that card into your hand unless any opponent pays 3 life", run once per card.
            val offerToHand = Effects.UnlessAnyPlayerPays(
                cost = Costs.pay.PayLife(3),
                effect = Effects.Move(EffectTarget.IterationEntity, Zone.HAND),
                eligiblePlayers = Player.EachOpponent
            )
            val (first, afterFirst) = chooseExactlySplit(
                1, from = top,
                prompt = "Choose the card opponents decide on first"
            )
            reveal(first)
            run(Effects.ForEachInCollection(first, offerToHand))
            val (second, third) = chooseExactlySplit(
                1, from = afterFirst,
                prompt = "Choose the card opponents decide on next"
            )
            reveal(second)
            run(Effects.ForEachInCollection(second, offerToHand))
            reveal(third)
            run(Effects.ForEachInCollection(third, offerToHand))

            // "Then exile the rest."
            exile(filter(top, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Eric Deschamps"
        flavorText = "\"Take my blood if you want. Just don't take my gold!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25750098-02a1-48e4-917d-187c41e111b6.jpg?1783935754"
        ruling("2017-09-29", "You reveal all three cards before opponents choose whether to pay life for any of them.")
        ruling("2017-09-29", "In a multiplayer game, each opponent in turn order chooses whether to pay life for one card before proceeding to the next card. You choose the order to perform this process for the cards, but opponents may discuss them before making any choices. Opponents will then know choices made by earlier opponents when making their choices.")
    }
}
