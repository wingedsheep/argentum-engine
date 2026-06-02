package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Pull Through the Weft
 * {3}{G}{G}
 * Sorcery
 * Return up to two target nonland permanent cards from your graveyard to your hand, then
 * return up to two target land cards from your graveyard to the battlefield tapped.
 *
 * Two independent cast-time `targets("…", optional = true)` calls give the player two
 * prompts (each "up to two") with their own legal-target lists. At resolution the engine
 * places every chosen target into `context.targets` as a flat list; the effect gathers
 * those via `CardSource.ChosenTargets` and partitions them by `GameObjectFilter.Land` —
 * lands into `chosenLands`, the rest (the nonland permanent cards from the first prompt)
 * into `chosenNonlands`. Each pile then routes to its own destination (hand vs.
 * battlefield-tapped), keeping the two-target-group routing accurate even when the per-
 * requirement chosen counts differ.
 */
val PullThroughTheWeft = card("Pull Through the Weft") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return up to two target nonland permanent cards from your graveyard to your hand, " +
        "then return up to two target land cards from your graveyard to the battlefield tapped."

    spell {
        targets(
            "nonland permanent card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.NonlandPermanent.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
                count = 2,
                optional = true,
            ),
        )
        targets(
            "land card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Land.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
                count = 2,
                optional = true,
            ),
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ChosenTargets,
                    storeAs = "chosen",
                ),
                FilterCollectionEffect(
                    from = "chosen",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                    storeMatching = "chosenLands",
                    storeNonMatching = "chosenNonlands",
                ),
                MoveCollectionEffect(
                    from = "chosenNonlands",
                    destination = CardDestination.ToZone(Zone.HAND),
                ),
                MoveCollectionEffect(
                    from = "chosenLands",
                    destination = CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped,
                    ),
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a70d0877-1a1e-436b-bf8c-0ff6df9efc6a.jpg?1752947378"
    }
}
