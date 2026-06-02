package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Fireglass Mentor
 * {B}{R}
 * Creature — Lizard Warlock
 * 2/1
 *
 * At the beginning of your second main phase, if an opponent lost life this turn,
 * exile the top two cards of your library. Choose one of them. Until end of turn,
 * you may play that card.
 */
val FireglassMentor = card("Fireglass Mentor") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Lizard Warlock"
    oracleText = "At the beginning of your second main phase, if an opponent lost life this turn, " +
        "exile the top two cards of your library. Choose one of them. Until end of turn, you may play that card."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.YourPostcombatMain
        triggerCondition = Conditions.OpponentLostLifeThisTurn
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                    storeAs = "exiled",
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE),
                ),
                SelectFromCollectionEffect(
                    from = "exiled",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "chosen",
                    prompt = "Choose a card you may play this turn",
                ),
                GrantMayPlayFromExileEffect(from = "chosen"),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Henry Peters"
        flavorText = "\"Nothing focuses the mind quite like a flame.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b78fbaa3-c580-4290-9c28-b74169aab2fc.jpg?1721427057"
    }
}
