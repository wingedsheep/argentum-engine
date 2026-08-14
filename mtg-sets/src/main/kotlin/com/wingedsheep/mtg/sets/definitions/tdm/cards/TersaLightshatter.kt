package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tersa Lightshatter — Tarkir: Dragonstorm #127
 * {2}{R} · Legendary Creature — Orc Wizard · Rare
 * 3/3
 *
 * Haste
 * When Tersa Lightshatter enters, discard up to two cards, then draw that many cards.
 * Whenever Tersa Lightshatter attacks, if there are seven or more cards in your graveyard, exile a
 * card at random from your graveyard. You may play that card this turn.
 */
val TersaLightshatter = card("Tersa Lightshatter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Orc Wizard"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "When Tersa Lightshatter enters, discard up to two cards, then draw that many cards.\n" +
        "Whenever Tersa Lightshatter attacks, if there are seven or more cards in your graveyard, " +
        "exile a card at random from your graveyard. You may play that card this turn."

    keywords(Keyword.HASTE)

    // ETB loot run backwards: discard up to two, then draw that many (declining discards draws zero).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Hand.discardUpToThenDraw(2)
        description = "When Tersa Lightshatter enters, discard up to two cards, then draw that many cards."
    }

    // Attack trigger gated by an intervening "if" (seven or more cards in your graveyard). On
    // resolution, exile a random card from your graveyard and grant permission to play it this turn.
    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.CardsInGraveyardAtLeast(7)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You),
                    storeAs = "graveyardExile"
                ),
                SelectFromCollectionEffect(
                    from = "graveyardExile",
                    selection = SelectionMode.Random(DynamicAmount.Fixed(1)),
                    storeSelected = "exiledAtRandom"
                ),
                MoveCollectionEffect(
                    from = "exiledAtRandom",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.You)
                ),
                GrantMayPlayFromExileEffect("exiledAtRandom", MayPlayExpiry.EndOfTurn)
            )
        )
        description = "Whenever Tersa Lightshatter attacks, if there are seven or more cards in your " +
            "graveyard, exile a card at random from your graveyard. You may play that card this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99e96b34-b1c4-4647-a38e-2cf1aedaaace.jpg?1743204474"
    }
}
