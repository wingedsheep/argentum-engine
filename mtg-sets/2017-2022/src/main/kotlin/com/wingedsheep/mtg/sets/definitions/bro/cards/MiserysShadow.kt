package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Misery's Shadow — The Brothers' War #107 (canonical printing)
 * {1}{B} · Creature — Shade · 2/2
 *
 * If a creature an opponent controls would die, exile it instead.
 * {1}: This creature gets +1/+1 until end of turn.
 *
 * The replacement is the same [RedirectZoneChange] Stone of Erech and Vren use.
 */
val MiserysShadow = card("Misery's Shadow") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Shade"
    power = 2
    toughness = 2
    oracleText = "If a creature an opponent controls would die, exile it instead.\n" +
        "{1}: This creature gets +1/+1 until end of turn."

    // If a creature an opponent controls would die, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            )
        )
    )

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "{1}: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Igor Kieryluk"
        flavorText = "In the ruins of Kroog, only anguish thrived."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/514c5e77-9ecc-41c9-b104-76343601fafb.jpg?1783920084"
        ruling(
            "2022-10-14",
            "Because creatures controlled by opponents aren't being put into a graveyard, any \"when [this creature] dies\" triggered abilities those creatures have won't trigger."
        )
    }
}
