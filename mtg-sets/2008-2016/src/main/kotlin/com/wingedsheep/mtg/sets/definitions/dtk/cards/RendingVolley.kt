package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rending Volley
 * {R}
 * Instant
 * This spell can't be countered.
 * Rending Volley deals 4 damage to target white or blue creature.
 *
 * "This spell can't be countered" is the intrinsic [cantBeCountered] flag. The colour restriction
 * rides the target filter, so a creature that stops being white or blue before resolution makes
 * the spell fizzle.
 */
val RendingVolley = card("Rending Volley") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "Rending Volley deals 4 damage to target white or blue creature."

    cantBeCountered = true

    spell {
        val t = target(TargetFilter(GameObjectFilter.Creature.withAnyColor(Color.WHITE, Color.BLUE)))
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Lucas Graciano"
        flavorText = "The sky offers few hiding places."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8234090e-9df1-4915-90ef-8a4bc6212655.jpg?1783938587"
    }
}
