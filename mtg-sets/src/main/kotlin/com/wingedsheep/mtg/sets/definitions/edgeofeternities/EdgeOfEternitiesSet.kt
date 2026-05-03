package com.wingedsheep.mtg.sets.definitions.edgeofeternities

import com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards.*

/**
 * Edge of Eternities Set (2025)
 *
 * Set Code: EOE
 * Release Date: August 1, 2025
 * Card Count: 261
 */
object EdgeOfEternitiesSet {

    const val SET_CODE = "EOE"
    const val SET_NAME = "Edge of Eternities"

    val basicLands = EdgeOfEternitiesBasicLands.map { it.copy(setCode = SET_CODE) }

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        Annul,
        ArchenemysCharm,
        AtomicMicrosizer,
        BiosynthicBurst,
        BloomingStinger,
        Bombard,
        BreedingPool,
        CerebralDownload,
        ChromeCompanion,
        CloudsculptTechnician,
        CometCrawler,
        CryogenRelic,
        Cryoshatter,
        DarkEndurance,
        DawnstrikeVanguard,
        Depressurize,
        DiplomaticRelations,
        DualSunAdepts,
        DualSunTechnique,
        DubiousDelicacy,
        EumidianTerrabotanist,
        ExosuitSavior,
        GodlessShrine,
        GravbladeHeavy,
        HardlightContainment,
        SacredFoundry,
        SeamRip,
        SledgeClassSeedship,
        StompingGround,
        WateryGrave,

        // Basic lands
    ) + EdgeOfEternitiesBasicLands
}
