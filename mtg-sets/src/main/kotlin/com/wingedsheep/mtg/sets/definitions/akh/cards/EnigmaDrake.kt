package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Enigma Drake
 * {1}{U}{R}
 * Creature — Drake
 * Power/toughness: star/4
 *
 * Flying
 * Enigma Drake's power is equal to the number of instant and sorcery cards in your graveyard.
 *
 * The characteristic-defining power is a `dynamicPower(...)` over
 * [DynamicAmount.Count] of instant/sorcery cards in the controller's graveyard
 * ([GameObjectFilter.InstantOrSorcery]). Only power is dynamic; toughness stays a printed 4,
 * so we use the single-stat `dynamicPower(...)` helper (same shape as Duelist of the Mind).
 *
 * Canonical printing lives here (Amonkhet, the earliest real expansion); later sets
 * (M19, Foundations, …) contribute only [com.wingedsheep.sdk.model.Printing] rows.
 */
val EnigmaDrake = card("Enigma Drake") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Drake"
    toughness = 4
    oracleText = "Flying\nEnigma Drake's power is equal to the number of instant and sorcery cards in your graveyard."

    dynamicPower(
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery)
    )

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Steve Argyle"
        flavorText = "Many initiates believe it possesses secrets known only to Kefnet himself. Many have become meals trying to learn them."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66286631-c16e-410c-b963-25cfe8005d8f.jpg?1782711097"
    }
}
