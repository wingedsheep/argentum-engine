package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Structural Distortion — Shadows over Innistrad #185 (canonical printing)
 * {3}{R} · Sorcery
 *
 * Exile target artifact or land. Structural Distortion deals 2 damage to that permanent's controller.
 *
 * The exile runs first; "that permanent's controller" is then read via last-known information
 * (CR 608.2h) from the battlefield-exit snapshot, so an opponent's stolen artifact damages the player
 * who controlled it, not its owner. An illegal target fizzles the whole spell — no damage.
 */
val StructuralDistortion = card("Structural Distortion") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile target artifact or land. Structural Distortion deals 2 damage to that permanent's controller."

    spell {
        val permanent = target(TargetFilter.ArtifactOrLand)
        effect = Effects.Exile(permanent) then Effects.DealDamage(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Jung Park"
        flavorText = "\"As Zendikar has bled, so will Innistrad. As I have wept, so will Sorin.\"\n—Nahiri"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7895890-a774-4c7c-9f15-78b8aadfd9ef.jpg?1783937741"
    }
}
