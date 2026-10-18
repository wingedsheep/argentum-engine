package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Petra Sphinx
 * {2}{W}{W}{W}
 * Creature — Sphinx
 * 3/4
 *
 * {T}: Target player chooses a card name, then reveals the top card of their library. If that card
 * has the chosen name, that player puts it into their hand. If it doesn't, the player puts it into
 * their graveyard.
 *
 * The target player names the card ([Chooser.TargetPlayer]); the revealed card is split by that
 * name, the match going to their hand and a miss to their graveyard.
 */
val PetraSphinx = card("Petra Sphinx") {
    manaCost = "{2}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Sphinx"
    power = 3
    toughness = 4
    oracleText = "{T}: Target player chooses a card name, then reveals the top card of their library. " +
        "If that card has the chosen name, that player puts it into their hand. If it doesn't, the " +
        "player puts it into their graveyard."

    activatedAbility {
        cost = Costs.Tap
        target(Targets.Player)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(chooser = Chooser.TargetPlayer)
            val revealed = gather(CardSource.TopOfLibrary(1, Player.TargetPlayer), revealed = true)
            val (match, miss) = selectAllSplit(
                from = revealed,
                filter = GameObjectFilter.Any.namedFromVariable(chosenName)
            )
            toHand(match, Player.TargetPlayer)
            toGraveyard(miss, Player.TargetPlayer)
        }
        description = "{T}: Target player chooses a card name, then reveals the top card of their " +
            "library. If that card has the chosen name, that player puts it into their hand. If it " +
            "doesn't, the player puts it into their graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ef99f07-c987-451a-b18a-2719eea654cd.jpg?1783948083"
    }
}
