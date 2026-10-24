package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mage Hunters' Onslaught — Strixhaven: School of Mages #77 (canonical printing)
 * {2}{B}{B} · Sorcery
 *
 * Destroy target creature or planeswalker.
 * Whenever a creature blocks this turn, its controller loses 1 life.
 *
 * The rider is Noxious Assault's filter-scoped delayed trigger over `Triggers.a(Creature).blocks()`,
 * one trigger per declared blocker for the rest of the turn, with
 * [EffectTarget.ControllerOfTriggeringEntity] naming each blocker's controller.
 */
val MageHuntersOnslaught = card("Mage Hunters' Onslaught") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or planeswalker.\n" +
        "Whenever a creature blocks this turn, its controller loses 1 life."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t) then Effects.CreateDelayedTrigger(
            effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity),
            trigger = Triggers.a(GameObjectFilter.Creature).blocks(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Lie Setiawan"
        flavorText = "Extus stood before his alma mater for a moment, remembering first his studies, " +
            "then his vengeance. At his command, the mage hunters unfolded."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed85140f-f0e0-4ac1-a67f-26d17ff95e31.jpg?1783927365"
    }
}
