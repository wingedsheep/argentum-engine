package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Ghalta, Stampede Tyrant
 * {5}{G}{G}{G}
 * Legendary Creature — Elder Dinosaur
 * 12/12
 *
 * Trample
 * When Ghalta enters, put any number of creature cards from your hand
 * onto the battlefield.
 */
val GhaltaStampedeTyrant = card("Ghalta, Stampede Tyrant") {
    manaCost = "{5}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elder Dinosaur"
    power = 12
    toughness = 12
    oracleText = "Trample\nWhen Ghalta enters, put any number of creature cards from your hand onto the battlefield."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                storeAs = "ghalta_candidates"
            ),
            SelectFromCollectionEffect(
                from = "ghalta_candidates",
                selection = SelectionMode.ChooseAnyNumber,
                storeSelected = "ghalta_putting",
                prompt = "Choose any number of creature cards to put onto the battlefield"
            ),
            MoveCollectionEffect(
                from = "ghalta_putting",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
            )
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "185"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72e805e9-69be-45c1-aa04-f460641a0c1e.jpg?1699044395"
    }
}
