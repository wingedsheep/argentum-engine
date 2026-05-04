package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Hemosymbic Mite
 * {G}
 * Creature — Mite
 * Whenever this creature becomes tapped, another target creature you control gets +X/+X until end of turn, where X is this creature's power.
 */
val HemosymbicMite = card("Hemosymbic Mite") {
    manaCost = "{G}"
    typeLine = "Creature — Mite"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature becomes tapped, another target creature you control gets +X/+X until end of turn, where X is this creature's power."

    // Whenever this creature becomes tapped, another target creature you control gets +X/+X until end of turn, where X is this creature's power
    triggeredAbility {
        trigger = Triggers.BecomesTapped
        val target = target("another target creature you control", Targets.CreatureYouControl)
        val powerBonus = DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power)
        effect = Effects.ModifyStats(powerBonus, powerBonus, target)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Amanda Lee"
        flavorText = "By stimulating violent behavior in its host, the mite scavenges the remains of prey much too large for it to hunt on its own."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c14137a4-2d44-444c-ad50-e2edf9380571.jpg?1752947330"
    }
}
