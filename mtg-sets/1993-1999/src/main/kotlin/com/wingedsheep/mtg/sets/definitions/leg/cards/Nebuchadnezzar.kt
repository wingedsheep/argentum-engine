package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Nebuchadnezzar
 * {3}{U}{B}
 * Legendary Creature — Human Wizard
 * 3/3
 *
 * {X}, {T}: Choose a card name. Target opponent reveals X cards at random from their hand. Then
 * that player discards all cards with that name revealed this way. Activate only during your turn.
 *
 * The name is chosen on resolution (per the 2009-10-01 ruling), before the random reveal — the
 * Petra Sphinx shape with Mind Twist's X-sized random pick: gather the opponent's hand, pick X at
 * random, reveal them, then discard only the revealed cards matching the chosen name. X larger
 * than the hand reveals the whole hand.
 */
val Nebuchadnezzar = card("Nebuchadnezzar") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "{X}, {T}: Choose a card name. Target opponent reveals X cards at random from " +
        "their hand. Then that player discards all cards with that name revealed this way. " +
        "Activate only during your turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        val victim = target(Targets.Opponent)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName()
            val hand = gather(CardSource.FromZone(Zone.HAND, victim.asPlayer))
            val revealed = chooseRandom(DynamicAmounts.xValue(), from = hand)
            reveal(revealed)
            val matches = selectAll(
                from = revealed,
                filter = GameObjectFilter.Any.namedFromVariable(chosenName)
            )
            discard(matches, victim.asPlayer)
        }
        description = "{X}, {T}: Choose a card name. Target opponent reveals X cards at random " +
            "from their hand. Then that player discards all cards with that name revealed this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3f17ce3-711b-4bd9-addf-dd440fa7d2b7.jpg?1783948035"
        ruling("2009-10-01", "You don't name a card until the ability resolves. Once you name a card, it's too late for the targeted opponent to respond.")
        ruling("2009-10-01", "You may activate the ability only during your turn, but you may do so any time you have priority. (In other words, it's not restricted to only the times you could cast a sorcery.)")
    }
}
