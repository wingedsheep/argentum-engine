package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CapCounterPlacementThisTurn
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Melira, the Living Cure
 * {G}{W}
 * Legendary Creature — Human Scout
 * 3/3
 * If you would get one or more poison counters, instead you get one poison counter and you can't
 * get additional poison counters this turn.
 * Exile Melira: Choose another target creature or artifact. When it's put into a graveyard this
 * turn, return that card to the battlefield under its owner's control.
 *
 * The first ability is [CapCounterPlacementThisTurn]'s default shape (poison, you, cap 1). The
 * second is an event-based delayed trigger watching the target, returning the triggering card from
 * its owner's graveyard.
 */
val MeliraTheLivingCure = card("Melira, the Living Cure") {
    manaCost = "{G}{W}"
    typeLine = "Legendary Creature — Human Scout"
    power = 3
    toughness = 3
    oracleText = "If you would get one or more poison counters, instead you get one poison counter " +
        "and you can't get additional poison counters this turn.\n" +
        "Exile Melira: Choose another target creature or artifact. When it's put into a graveyard " +
        "this turn, return that card to the battlefield under its owner's control."

    replacementEffect(CapCounterPlacementThisTurn())

    activatedAbility {
        cost = Costs.ExileSelf
        val chosen = target(TargetFilter.CreatureOrArtifact.other())
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.self.dies(),
            watchedTarget = chosen,
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.Move(EffectTarget.TriggeringEntity, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        )
        description = "Exile Melira: Choose another target creature or artifact. When it's put into a " +
            "graveyard this turn, return that card to the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1329ad9-5989-4920-b17b-943b9b4a0cd9.jpg?1783917999"
    }
}
