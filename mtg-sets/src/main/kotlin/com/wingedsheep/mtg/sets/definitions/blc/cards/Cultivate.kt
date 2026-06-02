package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Cultivate {2}{G}
 * Sorcery
 *
 * Search your library for up to two basic land cards, reveal those cards,
 * put one onto the battlefield tapped and the other into your hand, then shuffle.
 *
 * Per Wizards ruling (2010-08-15): if you find only one basic land card, you
 * put it onto the battlefield tapped (the [SelectionMode.ChooseExactly] split
 * step auto-selects the only card when the collection has size 1).
 */
val Cultivate = card("Cultivate") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic land cards, reveal those cards, " +
        "put one onto the battlefield tapped and the other into your hand, then shuffle."

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    storeSelected = "found",
                    prompt = "Search your library for up to two basic land cards"
                ),
                SelectFromCollectionEffect(
                    from = "found",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "toBattlefield",
                    storeRemainder = "toHand",
                    selectedLabel = "Onto the battlefield tapped",
                    remainderLabel = "Into your hand",
                    prompt = "Choose which basic land enters the battlefield tapped; the other goes to your hand."
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    revealed = true
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true
                ),
                ShuffleLibraryEffect()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "212"
        artist = "Justyna Dura"
        flavorText = "All seeds share a common bond, calling to each other across infinity."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d378437-4fc1-4004-a49a-98d7bac36ef5.jpg?1721429237"
        ruling("2010-08-15", "If you choose to find only one basic land card, you put it onto the battlefield tapped.")
    }
}
