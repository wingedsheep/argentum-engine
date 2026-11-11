package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode

/**
 * Retreat to Emeria
 * {3}{W}
 * Enchantment
 * Landfall — Whenever a land you control enters, choose one —
 * • Create a 1/1 white Kor Ally creature token.
 * • Creatures you control get +1/+1 until end of turn.
 *
 * A modal triggered ability; the second mode affects only creatures you control as it resolves.
 */
val RetreatToEmeria = card("Retreat to Emeria") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, choose one —\n" +
        "• Create a 1/1 white Kor Ally creature token.\n" +
        "• Creatures you control get +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Kor", "Ally"),
                    imageUri = "https://cards.scryfall.io/normal/front/b/e/be224180-a482-4b94-8a9d-3a92ee0eb34b.jpg?1783938120",
                ),
                "Create a 1/1 white Kor Ally creature token",
            ),
            Mode.noTarget(
                Patterns.Group.modifyStatsForAll(1, 1, Filters.Group.creaturesYouControl),
                "Creatures you control get +1/+1 until end of turn",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b65101f-808a-40b8-8864-ee84c00d2496.jpg?1783938216"
    }
}
