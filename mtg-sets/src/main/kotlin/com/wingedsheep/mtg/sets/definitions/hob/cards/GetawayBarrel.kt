package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Getaway Barrel — The Hobbit #98
 * {3}{R} · Artifact · Rare
 *
 * When this artifact is put into a graveyard from the battlefield, reveal the top thirteen cards
 * of your library. Put a random creature card from among them onto the battlefield. Put the rest
 * on the bottom of your library in a random order.
 *
 * The Gishath, Sun's Avatar pipeline with the player choice swapped for the engine's own roll:
 * gather the top thirteen with `revealed = true`, then [SelectionMode.Random]`(1)` narrowed to
 * [GameObjectFilter.Creature] — the executor draws from the *eligible* (creature) subset only, and
 * the remainder collection keeps every card that wasn't picked, non-creatures included. A library
 * with fewer than thirteen cards reveals what's there; no creature among them selects nothing and
 * all thirteen go to the bottom.
 */
val GetawayBarrel = card("Getaway Barrel") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "When this artifact is put into a graveyard from the battlefield, reveal the top " +
        "thirteen cards of your library. Put a random creature card from among them onto the " +
        "battlefield. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.PutIntoGraveyardFromBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(13), Player.You),
                    storeAs = "barrel_revealed",
                    revealed = true
                ),
                SelectFromCollectionEffect(
                    from = "barrel_revealed",
                    selection = SelectionMode.Random(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Creature,
                    storeSelected = "barrel_toBattlefield",
                    storeRemainder = "barrel_toBottom"
                ),
                MoveCollectionEffect(
                    from = "barrel_toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                ),
                MoveCollectionEffect(
                    from = "barrel_toBottom",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
        description = "When this artifact is put into a graveyard from the battlefield, reveal the " +
            "top thirteen cards of your library. Put a random creature card from among them onto " +
            "the battlefield. Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Pablo Mendoza"
        flavorText = "\"I do hope I put the lids on tight enough!\"\n—Bilbo"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4819aa6-5d28-4a37-942d-89523e30c4e1.jpg?1785323241"
    }
}
