package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Conch Horn
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from your hand on top of your
 * library.
 *
 * The put-back is from the *whole* hand, not just the two cards drawn, and it is mandatory —
 * so a player who draws into an empty hand still returns one of the two.
 */
val ConchHorn = card("Conch Horn") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from your hand on top of your library."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Composite(
            Effects.DrawCards(2),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any),
                storeAs = "hand"
            ),
            SelectFromCollectionEffect(
                from = "hand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "toTop",
                selectedLabel = "Put on top of your library"
            ),
            MoveCollectionEffect(
                from = "toTop",
                destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Top)
            )
        )
        description = "{1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from your hand on top of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Phil Foglio"
        flavorText = "Even the most skilled of modern mages only partially understand the Conch Horn's awesome powers."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/860a9ba3-e4c4-4af9-bdfe-1ada39289fd5.jpg?1783947881"
    }
}
