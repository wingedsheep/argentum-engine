package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Infernal Caretaker
 * {3}{B}
 * Creature — Human Cleric
 * 2/2
 * Morph {3}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, return all Zombie cards from all graveyards to their owners' hands.
 */
val InfernalCaretaker = card("Infernal Caretaker") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Morph {3}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, return all Zombie cards from all graveyards to their owners' hands."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any.withSubtype("Zombie")),
                    storeAs = "zombies"
                ),
                MoveCollectionEffect(
                    from = "zombies",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You)
                )
            )
        )
    }

    morph = "{3}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a028a30-6242-4d87-9501-d1826ecb69b0.jpg?1562922909"
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
    }
}
