package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

private const val ELSPETH_EMBLEM = "Creatures you control get +2/+2 and have flying."

/**
 * Elspeth, Sun's Champion - {4}{W}{W}
 * Legendary Planeswalker — Elspeth
 * Starting Loyalty: 4
 *
 * +1: Create three 1/1 white Soldier creature tokens.
 * −3: Destroy all creatures with power 4 or greater.
 * −7: You get an emblem with "Creatures you control get +2/+2 and have flying."
 */
val ElspethSunsChampion = card("Elspeth, Sun's Champion") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Elspeth"
    startingLoyalty = 4
    oracleText = "+1: Create three 1/1 white Soldier creature tokens.\n" +
        "−3: Destroy all creatures with power 4 or greater.\n" +
        "−7: You get an emblem with \"$ELSPETH_EMBLEM\""

    // +1: Create three 1/1 white Soldier creature tokens.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            count = 3
        )
    }

    // −3: Destroy all creatures with power 4 or greater.
    loyaltyAbility(-3) {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.powerAtLeast(4))
    }

    // −7: You get an emblem with "Creatures you control get +2/+2 and have flying."
    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter.AllCreaturesYouControl,
            powerBonus = 2,
            toughnessBonus = 2,
            grantedKeywords = listOf(Keyword.FLYING.name),
            emblemDescription = ELSPETH_EMBLEM,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd5b1633-c41d-42b1-af1b-4a872077ffbd.jpg?1783939818"
    }
}
