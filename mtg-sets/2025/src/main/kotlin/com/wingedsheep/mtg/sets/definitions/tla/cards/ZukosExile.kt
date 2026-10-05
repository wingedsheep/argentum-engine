package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zuko's Exile
 * {5}
 * Instant — Lesson
 *
 * Exile target artifact, creature, or enchantment. Its controller creates a Clue token.
 * (It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 *
 * "Its controller" is the exiled permanent's controller, resolved via
 * [EffectTarget.TargetController] (last-known information after the exile).
 */
val ZukosExile = card("Zuko's Exile") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Instant — Lesson"
    oracleText = "Exile target artifact, creature, or enchantment. Its controller creates a Clue token. " +
        "(It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.ArtifactCreatureOrEnchantment))
        effect = Effects.Exile(permanent) then
            Effects.CreateClue(controller = EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Eiji Kaneda"
        flavorText = "As punishment for his disobedience, Zuko was banished and sent to capture the Avatar. " +
            "Only then could he return home with his honor."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9090b055-3406-4fed-a8c6-3f6353a9600e.jpg?1778833154"
    }
}
