package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Gastal Blockbuster — Aetherdrift #128
 * {2}{R} · Creature — Human Berserker · 3/2
 *
 * When this creature enters, you may sacrifice a creature or Vehicle. When you do, destroy
 * target artifact an opponent controls.
 *
 * Same shape as Ruthless Lawbringer: the optional "sacrifice a creature or Vehicle" is a
 * resolution-time choice (player accepts the optional, then chooses which permanent), and the
 * "When you do" reflexive trigger targets an opponent's artifact as it goes on the stack. The
 * sacrifice is *not* "another" (the source creature itself is an eligible sacrifice), so the
 * select filter is an unrestricted [GameObjectFilter.CreatureOrVehicle] you control.
 */
val GastalBlockbuster = card("Gastal Blockbuster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may sacrifice a creature or Vehicle. When you do, " +
        "destroy target artifact an opponent controls."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(
                            filter = TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl())
                        ),
                        storeAs = "toSacrifice"
                    ),
                    Effects.SacrificeTarget(EffectTarget.PipelineTarget("toSacrifice"))
                )
            ),
            optional = true,
            reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetPermanent(filter = TargetFilter.Artifact.opponentControls())
            ),
            descriptionOverride = "You may sacrifice a creature or Vehicle. When you do, destroy target " +
                "artifact an opponent controls."
        )
        description = "When this creature enters, you may sacrifice a creature or Vehicle. When you do, " +
            "destroy target artifact an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Bryan Sola"
        flavorText = "\"Consider me an accident no longer waiting to happen.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dca41ec6-8f8f-42ef-abac-cc645c6440b7.jpg?1782687857"
    }
}
