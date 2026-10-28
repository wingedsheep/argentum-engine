package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace's Archivist
 * {1}{U}{U}
 * Creature — Vedalken Wizard
 * 2/2
 * {U}, {T}: Each player discards their hand, then draws cards equal to the greatest number of
 * cards a player discarded this way.
 *
 * Windfall's shape on an activated ability: the greatest hand size is frozen with
 * [Effects.StoreNumber] before anyone discards (discarding a hand discards every card in it), then
 * every player discards their hand, then every player draws the frozen number.
 */
val JacesArchivist = card("Jace's Archivist") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Wizard"
    oracleText = "{U}, {T}: Each player discards their hand, then draws cards equal to the greatest number of cards a player discarded this way."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        effect = Effects.StoreNumber(
            "jacesArchivistGreatestDiscard",
            DynamicAmounts.greatestAmongPlayers(DynamicAmounts.cardsInYourHand(), Player.Each)
        ) then
            Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand()) then
            Effects.ForEachPlayer(
                Player.Each,
                Effects.DrawCards(DynamicAmounts.storedNumber("jacesArchivistGreatestDiscard"))
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "James Ryman"
        flavorText = "\"This is a sacred duty. Master Beleren's trust has bought an eternity of my loyalty.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47c6b294-3840-4007-a4e3-67309f6581dd.jpg?1783941092"
        ruling("2011-09-22", "If this ability causes a player to draw more cards than are left in their library, that player loses the game as a state-based action. If this ability causes all players to do this, the game is a draw.")
    }
}
