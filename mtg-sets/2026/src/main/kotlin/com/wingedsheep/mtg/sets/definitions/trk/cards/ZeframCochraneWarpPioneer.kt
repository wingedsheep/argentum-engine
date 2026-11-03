package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Zefram Cochrane, Warp Pioneer
 * {2}{U}
 * Legendary Creature — Human Pilot
 * 2/3
 *
 * You may look at the top card of your library any time.
 * Once during each of your turns, you may cast an artifact spell from the top of your library.
 * Spacecraft you control enter with two additional charge counters on them.
 *
 * "Once during each of your turns" is a once-per-turn [CastSpellTypesFromTopOfLibrary] gated by
 * [Conditions.IsYourTurn]; both legal-action enumeration and the cast-time re-check honour the gate.
 */
val ZeframCochraneWarpPioneer = card("Zefram Cochrane, Warp Pioneer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Pilot"
    power = 2
    toughness = 3
    oracleText = "You may look at the top card of your library any time.\n" +
        "Once during each of your turns, you may cast an artifact spell from the top of your library.\n" +
        "Spacecraft you control enter with two additional charge counters on them."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Artifact,
            maxCastsPerTurn = 1,
        )
    }

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.CHARGE,
            count = 2,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Permanent.withSubtype("Spacecraft").youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43ce2e91-9dce-4e86-a4b8-8d21e4e368ea.jpg?1793013553"
    }
}
