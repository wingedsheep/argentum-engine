package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * One with the Machine reprint in PIP. Canonical CardDefinition lives in Core Set 2019 (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.m19.cards.OneWithTheMachine`.
 */
val OneWithTheMachineReprint = Printing(
    oracleId = "757eadac-dd29-4a7f-8683-5bc823168326",
    name = "One with the Machine",
    setCode = "PIP",
    collectorNumber = "179",
    scryfallId = "0f555efc-4314-4111-8c61-9548c5bae2e0",
    artist = "Bartek Fedyczak",
    imageUri = "https://cards.scryfall.io/normal/front/0/f/0f555efc-4314-4111-8c61-9548c5bae2e0.jpg?1783912364",
    releaseDate = "2024-03-08",
    rarity = Rarity.RARE,
)
