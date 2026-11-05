package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana's Defeat
 * {B}
 * Sorcery
 * Destroy target black creature or black planeswalker. If that permanent was a Liliana
 * planeswalker, her controller loses 3 life.
 *
 * "Was" is read before the destroy: the gate checks the target while it is still on the
 * battlefield, and both branches destroy it, so the life loss doesn't depend on the destruction
 * succeeding (an indestructible Liliana still costs her controller 3 life). "Her controller" is
 * [EffectTarget.TargetController], which falls back to the permanent's last-known controller once
 * it has left the battlefield.
 */
val LilianasDefeat = card("Liliana's Defeat") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target black creature or black planeswalker. If that permanent was a Liliana " +
        "planeswalker, her controller loses 3 life."

    spell {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.withColor(Color.BLACK)))
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                victim,
                GameObjectFilter.Planeswalker.withSubtype(Subtype.LILIANA)
            ),
            then = Effects.Destroy(victim) then Effects.LoseLife(3, EffectTarget.TargetController),
            otherwise = Effects.Destroy(victim)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Kieran Yanner"
        flavorText = "\"All the power you have gathered for yourself, Liliana, I can take away.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f72b028-b9df-40c7-822f-4acc6bdcc719.jpg?1783936040"
    }
}
