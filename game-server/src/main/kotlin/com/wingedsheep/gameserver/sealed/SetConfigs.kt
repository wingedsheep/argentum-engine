package com.wingedsheep.gameserver.sealed

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.dominaria.DominariaSet
import com.wingedsheep.mtg.sets.definitions.edgeofeternities.EdgeOfEternitiesSet
import com.wingedsheep.mtg.sets.definitions.khans.KhansOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.legions.LegionsSet
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.LorwynEclipsedSet
import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet

/**
 * Application-level registry of set configurations used by the booster generator.
 *
 * This file lives in `game-server` rather than `rules-engine` because it
 * imports concrete card-set modules from `mtg-sets`, and `rules-engine` must
 * stay free of card-specific dependencies (see CLAUDE.md). The booster
 * generator itself is pure logic and lives in `rules-engine`; this object
 * is the consumer-side catalogue that wires specific sets into it.
 */
object SetConfigs {
    val portalSetConfig = BoosterGenerator.SetConfig(
        setCode = PortalSet.SET_CODE,
        setName = PortalSet.SET_NAME,
        cards = PortalSet.allCards,
        basicLands = PortalSet.basicLands
    )

    val onslaughtSetConfig = BoosterGenerator.SetConfig(
        setCode = OnslaughtSet.SET_CODE,
        setName = OnslaughtSet.SET_NAME,
        cards = OnslaughtSet.allCards,
        basicLands = OnslaughtSet.basicLands,
        block = "Onslaught"
    )

    val scourgeSetConfig = BoosterGenerator.SetConfig(
        setCode = ScourgeSet.SET_CODE,
        setName = ScourgeSet.SET_NAME,
        cards = ScourgeSet.allCards,
        basicLands = OnslaughtSet.basicLands, // Scourge has no basic lands; use Onslaught block lands
        block = "Onslaught"
    )

    val legionsSetConfig = BoosterGenerator.SetConfig(
        setCode = LegionsSet.SET_CODE,
        setName = LegionsSet.SET_NAME,
        cards = LegionsSet.allCards,
        basicLands = OnslaughtSet.basicLands, // Legions has no basic lands; use Onslaught block lands
        incomplete = false,
        block = "Onslaught",
        totalSetSize = 145
    )

    val khansSetConfig = BoosterGenerator.SetConfig(
        setCode = KhansOfTarkirSet.SET_CODE,
        setName = KhansOfTarkirSet.SET_NAME,
        cards = KhansOfTarkirSet.allCards,
        basicLands = KhansOfTarkirSet.basicLands,
        incomplete = false,
        totalSetSize = 249
    )

    val dominariaSetConfig = BoosterGenerator.SetConfig(
        setCode = DominariaSet.SET_CODE,
        setName = DominariaSet.SET_NAME,
        cards = DominariaSet.allCards,
        basicLands = DominariaSet.basicLands,
        incomplete = true,
        guaranteedLegendary = true
    )

    val bloomburrowSetConfig = BoosterGenerator.SetConfig(
        setCode = BloomburrowSet.SET_CODE,
        setName = BloomburrowSet.SET_NAME,
        cards = BloomburrowSet.allCards,
        basicLands = BloomburrowSet.basicLands,
        incomplete = false,
        totalSetSize = 272
    )

    val lorwynEclipsedSetConfig = BoosterGenerator.SetConfig(
        setCode = LorwynEclipsedSet.SET_CODE,
        setName = LorwynEclipsedSet.SET_NAME,
        cards = LorwynEclipsedSet.allCards,
        basicLands = LorwynEclipsedSet.basicLands,
        incomplete = true,
        totalSetSize = 273
    )

    val edgeOfEternitiesSetConfig = BoosterGenerator.SetConfig(
        setCode = EdgeOfEternitiesSet.SET_CODE,
        setName = EdgeOfEternitiesSet.SET_NAME,
        cards = EdgeOfEternitiesSet.allCards,
        basicLands = EdgeOfEternitiesSet.basicLands,
        incomplete = true
    )
}
