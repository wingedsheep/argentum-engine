package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Animal Magnetism
 * {4}{G}
 * Sorcery
 * Reveal the top five cards of your library. An opponent chooses a creature card
 * from among them. Put that card onto the battlefield and the rest into your graveyard.
 */
val AnimalMagnetism = card("Animal Magnetism") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. An opponent chooses a creature card from among them. Put that card onto the battlefield and the rest into your graveyard."

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                    storeAs = "revealed",
                    revealed = true
                ),
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Opponent,
                    filter = GameObjectFilter.Creature,
                    storeSelected = "chosen",
                    storeRemainder = "rest"
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c33db646-b30d-4a15-9f8a-63bda74e2d81.jpg?1562941108"
    }
}
