package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Volatile Fault
 * Land — Cave
 * {T}: Add {C}.
 * {1}, {T}, Sacrifice this land: Destroy target nonbasic land an opponent controls. That player may
 * search their library for a basic land card, put it onto the battlefield, then shuffle. You create
 * a Treasure token.
 *
 * The Demolition Field pattern with a cheaper ({1}) activation cost: the destroyed land's controller
 * gets the optional basic-land fetch, and instead of the activator fetching a land they create a
 * Treasure token.
 *
 * "That player" is [Player.ControllerOf] the destroyed land. The engine resolves it from the
 * target's last-known controller (CR 608.2h) once the land has left the battlefield, so a stolen
 * land credits its controller-at-death, not its owner; and if the land survives destruction
 * (indestructible), the reference resolves from the battlefield and the fetch still happens, as
 * printed. Their optional basic-land fetch runs under [Effects.ForEachPlayer], which rebinds
 * `Player.You` inside the search to that player.
 */
val VolatileFault = card("Volatile Fault") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Cave"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}, Sacrifice this land: Destroy target nonbasic land an opponent controls. " +
        "That player may search their library for a basic land card, put it onto the battlefield, " +
        "then shuffle. You create a Treasure token."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        val land = target(
            "target nonbasic land an opponent controls",
            TargetPermanent(filter = TargetFilter.NonbasicLand.opponentControls())
        )
        effect = Effects.Destroy(land) then
            Effects.ForEachPlayer(
                Player.ControllerOf("the destroyed land"),
                listOf(
                    MayEffect(
                        Patterns.Library.searchLibrary(
                            filter = GameObjectFilter.BasicLand,
                            count = 1,
                            destination = SearchDestination.BATTLEFIELD
                        )
                    )
                )
            ) then
            Effects.CreateTreasure(1)
        description = "{1}, {T}, Sacrifice this land: Destroy target nonbasic land an opponent " +
            "controls. That player may search their library for a basic land card, put it onto " +
            "the battlefield, then shuffle. You create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "286"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9385abf3-b067-4586-bf3d-175526cf8f0a.jpg?1782694383"
    }
}
