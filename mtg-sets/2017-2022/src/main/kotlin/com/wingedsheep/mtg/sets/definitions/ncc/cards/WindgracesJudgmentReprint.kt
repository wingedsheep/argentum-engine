package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Windgrace's Judgment reprint in New Capenna Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Commander 2018's `cards/` package; this
 * file contributes only the New Capenna Commander presentation row.
 */
val WindgracesJudgmentReprint = Printing(
    oracleId = "de1ca6ed-b275-4f62-ba05-f31b3659b352",
    name = "Windgrace's Judgment",
    setCode = "NCC",
    collectorNumber = "358",
    scryfallId = "0fd6469c-fbce-41ed-ae23-bb55c0f56d3d",
    artist = "Grzegorz Rutkowski",
    imageUri = "https://cards.scryfall.io/normal/front/0/f/0fd6469c-fbce-41ed-ae23-bb55c0f56d3d.jpg",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
