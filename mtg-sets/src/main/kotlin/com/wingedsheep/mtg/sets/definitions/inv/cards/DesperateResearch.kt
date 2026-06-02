package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
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
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Desperate Research
 * {1}{B}
 * Sorcery
 *
 * Choose a card name other than a basic land card name. Reveal the top seven cards of
 * your library and put all of them with that name into your hand. Exile the rest.
 *
 * Invasion engine gap #10 — the "name a card" half of the family. [Effects.ChooseCardName]
 * stores the chosen name in `chosenValues`; the reveal partitions the top seven by it via
 * [GameObjectFilter.namedFromVariable] (matches to hand, the rest exiled).
 */
val DesperateResearch = card("Desperate Research") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a card name other than a basic land card name. Reveal the top seven cards of " +
        "your library and put all of them with that name into your hand. Exile the rest."

    spell {
        effect = Effects.Composite(
            listOf(
                // 1. Name a card (no basic land card names).
                Effects.ChooseCardName(
                    storeAs = "chosenName",
                    prompt = "Choose a card name other than a basic land card name",
                    excludeBasicLandNames = true
                ),
                // 2. Reveal the top seven cards of your library.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(7), Player.You),
                    storeAs = "revealed",
                    revealed = true
                ),
                // 3. Partition: cards with the chosen name vs. the rest.
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.All,
                    filter = GameObjectFilter.Any.namedFromVariable("chosenName"),
                    storeSelected = "matches",
                    storeRemainder = "rest"
                ),
                // 4. Put all matches into your hand.
                MoveCollectionEffect(
                    from = "matches",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You)
                ),
                // 5. Exile the rest.
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.You)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a42ac7e-4a27-488c-a2e7-338b18103b02.jpg?1562916353"
    }
}
