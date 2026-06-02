package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Effects

/**
 * Skyskipper Duo
 * {4}{U}
 * Creature — Bird Frog
 * 3/3
 *
 * Flying
 * When this creature enters, exile up to one other target creature you control.
 * Return it to the battlefield under its owner's control at the beginning of the
 * next end step.
 */
val SkyskipperDuo = card("Skyskipper Duo") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Frog"
    oracleText = "Flying\nWhen this creature enters, exile up to one other target creature you control. " +
        "Return it to the battlefield under its owner's control at the beginning of the next end step."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "other creature you control",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.youControl()),
                optional = true
            )
        )
        effect = Effects.Composite(listOf(
            Effects.Move(creature, Zone.EXILE),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.Move(creature, Zone.BATTLEFIELD)
            )
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Mariah Tekulve"
        flavorText = "\"I don't know how to swim!\" said the bird. \"That's okay! I don't know how to fly!\" laughed the frog."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6844bad-ffbe-4c6e-b438-08562eccea52.jpg?1721426259"
    }
}
