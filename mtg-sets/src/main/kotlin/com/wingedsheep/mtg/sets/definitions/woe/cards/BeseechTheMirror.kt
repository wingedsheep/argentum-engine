package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Beseech the Mirror
 * {1}{B}{B}{B}
 * Sorcery
 *
 * Bargain
 * Search your library for a card, exile it face down, then shuffle. If this spell was
 * bargained, you may cast the exiled card for free if its mana value is 4 or less. Put it
 * into your hand if it wasn't cast this way.
 */
val BeseechTheMirror = card("Beseech the Mirror") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)\n" +
        "Search your library for a card, exile it face down, then shuffle. If this spell was " +
        "bargained, you may cast the exiled card without paying its mana cost if that spell's " +
        "mana value is 4 or less. Put the exiled card into your hand if it wasn't cast this way."

    bargain()

    spell {
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You),
                storeAs = "beseechLibrary"
            ),
            SelectFromCollectionEffect(
                from = "beseechLibrary",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "beseechFound",
                prompt = "Search your library for a card"
            ),
            MoveCollectionEffect(
                from = "beseechFound",
                destination = CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN,
                storeMovedAs = "beseechExiled"
            ),
            ShuffleLibraryEffect(),
            ConditionalEffect(
                condition = Conditions.WasBargained,
                effect = Effects.Composite(
                    FilterCollectionEffect(
                        from = "beseechExiled",
                        filter = CollectionFilter.ManaValueAtMost(DynamicAmount.Fixed(4)),
                        storeMatching = "beseechCastable"
                    ),
                    ConditionalOnCollectionEffect(
                        collection = "beseechCastable",
                        ifNotEmpty = MayEffect(
                            Effects.CastFromCollectionWithoutPayingCost("beseechCastable")
                        )
                    )
                )
            ),
            FilterCollectionEffect(
                from = "beseechExiled",
                filter = CollectionFilter.InZone(Zone.EXILE),
                storeMatching = "beseechUncast"
            ),
            MoveCollectionEffect(
                from = "beseechUncast",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "82"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18c59776-e1f1-4197-a128-db1d603f56b7.jpg?1783915111"

        ruling("2023-09-01", "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost.")
        ruling("2023-09-01", "If you copy a bargained spell, the copy is also bargained.")
    }
}
