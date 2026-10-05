package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Deathbringer Regent reprint in NCC. Canonical CardDefinition lives in Dragons of Tarkir (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.dtk.cards.DeathbringerRegent`.
 */
val DeathbringerRegentReprint = Printing(
    oracleId = "71b5fdf2-4475-4102-85eb-9be164a9ae29",
    name = "Deathbringer Regent",
    setCode = "NCC",
    collectorNumber = "246",
    scryfallId = "8006515a-69de-4713-8318-79c0ef78a6d1",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/8/0/8006515a-69de-4713-8318-79c0ef78a6d1.jpg?1783923269",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
