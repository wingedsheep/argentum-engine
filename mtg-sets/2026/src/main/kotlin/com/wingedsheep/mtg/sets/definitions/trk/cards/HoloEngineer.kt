package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.references.Player

private const val ILLUSION_TOKEN_ART =
    "https://cards.scryfall.io/normal/front/d/7/d760eee8-e49a-4662-8bfc-fcf8ed2ad2d8.jpg?1793364353"

/**
 * Holo-Engineer
 * {2}{G}{G}
 * Creature — Human Artificer
 * 3/3
 * When this creature enters, create a 3/3 colorless Illusion artifact creature token.
 * If one or more tokens would be created under your control, that many 3/3 colorless Illusion
 * artifact creature tokens are created instead.
 *
 * An unfiltered [ReplaceTokenCreationWithToken]: every token-creation event under your control —
 * Treasure, token copies, Roles — makes that many Illusions instead. Its own enters trigger makes
 * an Illusion either way; the substitute Illusion doesn't loop (CR 614.5).
 */
val HoloEngineer = card("Holo-Engineer") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, create a 3/3 colorless Illusion artifact creature token.\n" +
        "If one or more tokens would be created under your control, that many 3/3 colorless Illusion " +
        "artifact creature tokens are created instead."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3, toughness = 3,
            creatureTypes = setOf("Illusion"),
            artifactToken = true
        )
    }

    replacementEffect(
        ReplaceTokenCreationWithToken(
            tokens = listOf(Effects.CreateToken(
                power = 3, toughness = 3,
                creatureTypes = setOf("Illusion"),
                artifactToken = true,
                imageUri = ILLUSION_TOKEN_ART
            )),
            appliesTo = EventPattern.TokenCreationEvent(controller = Player.You)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Josu Hernaiz"
        flavorText = "\"This is a test run. I can assure you that the final product will be... zesty.\"\n—Dr. Lewis Zimmerman"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acfaf5f3-a4f5-418a-a29a-211b33bf18ee.jpg?1793100831"
    }
}
