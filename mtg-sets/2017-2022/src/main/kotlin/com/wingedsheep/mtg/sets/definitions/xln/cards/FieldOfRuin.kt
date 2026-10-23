package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Field of Ruin
 * Land
 * {T}: Add {C}.
 * {2}, {T}, Sacrifice this land: Destroy target nonbasic land an opponent controls. Each player
 * searches their library for a basic land card, puts it onto the battlefield, then shuffles.
 *
 * The Demolition Field shape, but the fetch is every player's (not optional, though a hidden-zone
 * search may always fail to find). [Effects.ForEachPlayer] over [Player.Each] rebinds `Player.You`
 * inside the search to each player in turn. Per the ruling, an indestructible target that survives
 * still lets everyone search; an illegal target fizzles the whole ability.
 */
val FieldOfRuin = card("Field of Ruin") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}, Sacrifice this land: Destroy target nonbasic land an opponent controls. " +
        "Each player searches their library for a basic land card, puts it onto the battlefield, " +
        "then shuffles."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        val land = target(TargetFilter.NonbasicLand.opponentControls())
        effect = Effects.Destroy(land) then
            Effects.ForEachPlayer(
                Player.Each,
                listOf(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.BasicLand,
                        count = 1,
                        destination = SearchDestination.BATTLEFIELD
                    )
                )
            )
        description = "{2}, {T}, Sacrifice this land: Destroy target nonbasic land an opponent " +
            "controls. Each player searches their library for a basic land card, puts it onto the " +
            "battlefield, then shuffles."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Dimitar Marinski"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d72afb21-7bb0-4fd8-a529-ada92a654f61.jpg?1783935697"
        ruling(
            "2021-03-19",
            "If the target land is an illegal target by the time Field of Ruin's ability tries to " +
                "resolve, the ability doesn't resolve. No players search for a basic land card or " +
                "shuffles. If the target is legal but not destroyed (most likely because it has " +
                "indestructible), each player does search and shuffle."
        )
    }
}
