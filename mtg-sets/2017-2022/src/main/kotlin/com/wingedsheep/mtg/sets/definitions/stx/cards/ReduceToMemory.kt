package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reduce to Memory — Strixhaven: School of Mages #25 (canonical printing)
 * {1}{W}{W} · Sorcery — Lesson
 *
 * Exile target nonland permanent. Its controller creates a 3/2 red and white Spirit creature
 * token.
 *
 * Introduction to Annihilation's exile with Beast Within's token for the target's controller
 * ([EffectTarget.TargetController]); the 3/2 red and white Spirit is the set's Lorehold token.
 */
val ReduceToMemory = card("Reduce to Memory") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery — Lesson"
    oracleText = "Exile target nonland permanent. Its controller creates a 3/2 red and white Spirit " +
        "creature token."

    spell {
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(t) then Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Spirit"),
            controller = EffectTarget.TargetController,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Campbell White"
        flavorText = "\"Do not flinch from studying the horrors of the past. History's most important " +
            "lessons are cloaked in terror and tragedy.\"\n—Augusta, Lorehold dean"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bfc408e-34d6-4e94-8678-4cc229cbc6f5.jpg?1783927387"
    }
}
