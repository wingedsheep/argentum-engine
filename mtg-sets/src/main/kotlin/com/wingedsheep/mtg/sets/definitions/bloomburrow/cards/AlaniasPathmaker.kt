package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Alania's Pathmaker
 * {3}{R}
 * Creature — Otter Wizard
 * 4/2
 * When this creature enters, exile the top card of your library. Until the end of your
 * next turn, you may play that card.
 */
val AlaniasPathmaker = card("Alania's Pathmaker") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Otter Wizard"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, exile the top card of your library. Until the end of your next turn, you may play that card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "exiledCard"
            ),
            MoveCollectionEffect(
                from = "exiledCard",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.UntilEndOfNextTurn)
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Jason Kang"
        flavorText = "\"The frogfolk should worry less that the sky is falling and worry more about where it's landing.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3871fe6-e26e-4ab4-bd81-7e3c7b8135c1.jpg?1721426565"
    }
}
