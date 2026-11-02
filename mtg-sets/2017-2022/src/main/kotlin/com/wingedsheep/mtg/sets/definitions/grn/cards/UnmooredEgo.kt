package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.OptionType

/**
 * Unmoored Ego
 * {1}{U}{B}
 * Sorcery
 * Choose a card name. Search target opponent's graveyard, hand, and library for up to four cards
 * with that name and exile them. That player shuffles, then draws a card for each card exiled from
 * their hand this way.
 *
 * Ancient Vendetta's up-to-four name search with Necromentia's hand-subset count: the hand cards
 * are filtered out of the selection *before* the exile so their count survives the move, and the
 * targeted opponent draws that many after shuffling.
 */
val UnmooredEgo = card("Unmoored Ego") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Choose a card name. Search target opponent's graveyard, hand, and library for up to four " +
        "cards with that name and exile them. That player shuffles, then draws a card for each card exiled " +
        "from their hand this way."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val chosenName = chooseOption(
                OptionType.CARD_NAME,
                prompt = "Choose a card name"
            )
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                search = true
            )
            val toExile = chooseUpTo(
                4, from = matches,
                prompt = "Exile up to four cards with the chosen name"
            )
            val exiledFromHand = filter(toExile, GameObjectFilter.Any.currentlyIn(Zone.HAND))
            exile(toExile, owner = opponent.asPlayer)
            run(Effects.ShuffleLibrary(target = opponent))
            run(Effects.DrawCards(exiledFromHand.count, target = opponent))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95aecc12-3363-41f7-9b58-277c81859670.jpg?1783934122"
    }
}
