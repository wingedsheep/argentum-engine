package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Gravkill
 * {3}{B}
 * Instant
 *
 * Exile target creature or Spacecraft.
 */
val Gravkill = card("Gravkill") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature or Spacecraft."

    spell {
        val target = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Permanent.withSubtype("Spacecraft"))))
        effect = Effects.Move(target, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Dominik Mayer"
        flavorText = "\"Sing! When the Faller reaches the end of his journey, all things will arrive at the Zero Point. What is unending will persist through to the new creation and be ordered accordingly.\"\n—*The Theorem Unending and Final,* Antiphon 6"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfa6c57f-a193-48cc-9764-d8348548a111.jpg?1753096760"
    }
}
