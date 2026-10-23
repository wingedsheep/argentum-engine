package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Ebondeath, Dracolich
 * {2}{B}{B}
 * Legendary Creature — Zombie Dragon
 * 5/2
 * Flash
 * Flying
 * Ebondeath enters tapped.
 * You may cast this card from your graveyard if a creature not named Ebondeath, Dracolich died this turn.
 */
val EbondeathDracolich = card("Ebondeath, Dracolich") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Dragon"
    power = 5
    toughness = 2
    oracleText = "Flash\nFlying\nEbondeath enters tapped.\n" +
        "You may cast this card from your graveyard if a creature not named Ebondeath, Dracolich died this turn."

    keywords(Keyword.FLASH, Keyword.FLYING)

    replacementEffect(EntersTapped())

    // Self-granted graveyard cast permission, gated on the game-wide death record: some creature
    // whose last-known name wasn't "Ebondeath, Dracolich" died this turn. Flash still applies.
    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            condition = Conditions.CreatureNotNamedDiedThisTurn("Ebondeath, Dracolich")
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "100"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6b3f460-92ba-4af0-aad9-cf86615d4177.jpg?1783926497"
    }
}
