package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tempted by the Oriq — Strixhaven: School of Mages #58 (canonical printing)
 * {1}{U}{U}{U} · Sorcery
 *
 * For each opponent, gain control of up to one target creature or planeswalker that player
 * controls with mana value 3 or less.
 *
 * The corpus's one-per-opponent distribution (Windgrace's Judgment / Tolarian Contempt): up to one
 * target per opponent, no two sharing a controller, then a permanent [Effects.GainControl] on each
 * (the control change lasts indefinitely, ruling).
 */
val TemptedByTheOriq = card("Tempted by the Oriq") {
    manaCost = "{1}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "For each opponent, gain control of up to one target creature or planeswalker " +
        "that player controls with mana value 3 or less."

    spell {
        targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls().manaValueAtMost(3)),
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(Effects.GainControl(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Billy Christian"
        flavorText = "To exact his vengeance, Extus needed followers. His infiltrators recruited " +
            "promising mage-students from the very halls he sought to destroy."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c23e64f9-531e-4735-9960-b0a76b32c340.jpg?1783927374"
        ruling("2021-04-16", "Tempted by the Oriq’s control-changing effect lasts indefinitely.")
        ruling("2021-04-16", "Unless it has haste, you will not able to attack with a creature or activate its abilities that have {T} in the cost on the turn you gain control of it.")
    }
}
