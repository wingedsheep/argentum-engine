package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * Curious Forager
 * {2}{G}
 * Creature — Squirrel Druid
 * 3/2
 *
 * When this creature enters, you may forage. When you do, return target permanent
 * card from your graveyard to your hand.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 *
 * Modeled as a reflexive trigger: the action is forage (modal: exile 3 or sacrifice Food),
 * optional, and the reflexive effect targets a permanent card in your graveyard to return to hand.
 * Target is selected AFTER foraging (reflexive trigger targeting).
 */
val CuriousForager = card("Curious Forager") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Squirrel Druid"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may forage. When you do, return target permanent card from your graveyard to your hand. (To forage, exile three cards from your graveyard or sacrifice a Food.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = MiscPatterns.forage(),
            optional = true,
            reflexiveEffect = Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetObject(filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
            ),
            hint = "Exile three cards from your graveyard or sacrifice a Food"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Mariah Tekulve"
        flavorText = "The bones of Calamity Beasts are sometimes buried in caches where their power can grow quietly."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64653b4a-e139-45f9-a915-ab49afb6b795.jpg?1721426795"
    }
}
