package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Exploration Broodship — Edge of Eternities Commander #14 (canonical printing)
 * {G} · Artifact — Spacecraft · 4/4
 *
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 3+ | You may play an additional land on each of your turns.
 * 8+ | Flying
 * Once during each of your turns, you may cast a permanent spell from your graveyard by
 * sacrificing a land in addition to paying its other costs.
 *
 * The graveyard-cast permission belongs to the 8+ block, so it is a conditional static that the
 * graveyard-cast scans resolve against the Spacecraft's charge counters.
 */
val ExplorationBroodship = card("Exploration Broodship") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Spacecraft"
    power = 4
    toughness = 4
    oracleText = "Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n3+ | You may play an additional land on each of your turns.\n8+ | Flying\nOnce during each of your turns, you may cast a permanent spell from your graveyard by sacrificing a land in addition to paying its other costs."

    station()

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 3)
        ability = GrantAdditionalLandDrop(count = 1)
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.NonlandPermanent,
            duringYourTurnOnly = true,
            oncePerTurn = true,
            additionalCost = Costs.additional.SacrificePermanent(GameObjectFilter.Land)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61596da3-2a0e-47d9-b707-8db4cce2479d.jpg?1783906063"
    }
}
