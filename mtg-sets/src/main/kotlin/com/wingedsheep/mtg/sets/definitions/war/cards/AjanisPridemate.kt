package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ajani's Pridemate reprint in WAR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the WAR-specific
 * presentation row.
 */
val AjanisPridemateReprint = Printing(
    oracleId = "95e94dea-5ac0-4d6f-adec-ca147aee861f",
    name = "Ajani's Pridemate",
    setCode = "WAR",
    collectorNumber = "4",
    scryfallId = "b3656310-093d-4724-a399-7f7010843b1f",
    artist = "Sidharth Chaturvedi",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b3656310-093d-4724-a399-7f7010843b1f.jpg?1557575878",
    releaseDate = "2019-05-03",
    rarity = Rarity.UNCOMMON,
)
