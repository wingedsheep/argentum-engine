package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Emberheart Challenger {1}{R}
 * Creature — Mouse Warrior
 * 2/2
 *
 * Haste
 * Prowess
 * Valiant — Whenever this creature becomes the target of a spell or ability you control
 * for the first time each turn, exile the top card of your library. Until end of turn,
 * you may play that card.
 */
val EmberheartChallenger = card("Emberheart Challenger") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Mouse Warrior"
    power = 2
    toughness = 2
    oracleText = "Haste\nProwess\nValiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, exile the top card of your library. Until end of turn, you may play that card."

    keywords(Keyword.HASTE)
    prowess()

    triggeredAbility {
        trigger = Triggers.Valiant
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "exiledCard"
            ),
            MoveCollectionEffect(
                from = "exiledCard",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCard")
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0035082e-bb86-4f95-be48-ffc87fe5286d.jpg?1721426609"
    }
}
