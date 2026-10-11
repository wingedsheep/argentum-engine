package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalTokenScale
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Academy Manufactor — Modern Horizons 2 #219
 * {3} · Artifact Creature — Assembly-Worker · 1/3
 *
 * If you would create a Clue, Food, or Treasure token, instead create one of each.
 *
 * Six [CreateAdditionalToken] entries, one per (created type, missing type) pair, each scaled
 * [AdditionalTokenScale.PER_MATCHING_TOKEN]: two Treasures become two of each. The entries are one
 * ability — they all judge the batch as it was before this Manufactor applied, so a Treasure's
 * added Clue doesn't itself add a Food — while a second Manufactor judges the grown batch, which
 * is the ruling's "three times that many" of each.
 */
val AcademyManufactor = card("Academy Manufactor") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 1
    toughness = 3
    oracleText = "If you would create a Clue, Food, or Treasure token, instead create one of each."

    val kinds = listOf("Clue", "Food", "Treasure")
    for (created in kinds) {
        for (added in kinds - created) {
            replacementEffect(
                CreateAdditionalToken(
                    additionalTokenType = added,
                    inheritTapped = true,
                    appliesTo = EventPattern.TokenCreationEvent(
                        tokenFilter = GameObjectFilter.Artifact.withSubtype(created)
                    ),
                    scale = AdditionalTokenScale.PER_MATCHING_TOKEN
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Campbell White"
        flavorText = "Automated systems at the Tolarian Academy sort new acquisitions for optimal use, determining which should be studied, eaten, or sold."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90744c2c-6dd7-4623-8cbf-b138b83ee88f.jpg?1783926807"
    }
}
