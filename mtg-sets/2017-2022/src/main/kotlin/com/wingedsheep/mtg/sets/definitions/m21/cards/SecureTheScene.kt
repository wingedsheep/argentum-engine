package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Secure the Scene — Core Set 2021 #35 (canonical printing)
 * {4}{W} · Sorcery
 *
 * Exile target nonland permanent. Its controller creates a 1/1 white Soldier creature token.
 *
 * Beast Within's shape with exile: the target's controller is read off its last-known information
 * once it has left. An illegal target fizzles the whole spell, so no Soldier (ruling).
 */
val SecureTheScene = card("Secure the Scene") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target nonland permanent. Its controller creates a 1/1 white Soldier creature token."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                controller = EffectTarget.TargetController,
                imageUri = "https://cards.scryfall.io/normal/front/1/b/1bdb2914-bba2-4cb6-802e-af2aeef46de8.jpg?1783930592"
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Zoltan Boros"
        flavorText = "\"You know it's a great show when the Azorius arrest the props, too!\"\n—Judith, the Scourge Diva"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6467d96-e43a-4b1e-b6ce-578d991077b5.jpg?1783930734"
        ruling(
            "2020-06-23",
            "If the target nonland permanent is an illegal target by the time Secure the Scene tries to " +
                "resolve, the spell won't resolve. No player will create a Soldier token."
        )
    }
}
