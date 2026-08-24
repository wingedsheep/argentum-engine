package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Seasinger
 * {1}{U}{U}
 * Creature — Merfolk
 * 0/1
 * When you control no Islands, sacrifice this creature.
 * You may choose not to untap this creature during your untap step.
 * {T}: Gain control of target creature whose controller controls an Island for as long as you
 * control this creature and this creature remains tapped.
 *
 * Old Man of the Sea's engine, with two clauses of its own:
 *  - the targeting restriction is about the *creature's* side of the table, not Seasinger's, so
 *    it uses `controllerControls` — a state predicate whose nested filter binds "you" to the
 *    candidate's controller.
 *  - the duration prints both halves, which is [Duration.WhileYouControlSourceAndSourceTapped]:
 *    untapping Seasinger hands the creature back, and so does an opponent stealing Seasinger.
 *    Per CR 611.2b it is one-way — re-tapping does not re-steal.
 *
 * "When you control no Islands" is a state trigger (CR 603.8), so it fires the moment the last
 * Island leaves and not once per upkeep.
 */
val Seasinger = card("Seasinger") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    oracleText = "When you control no Islands, sacrifice this creature.\n" +
        "You may choose not to untap this creature during your untap step.\n" +
        "{T}: Gain control of target creature whose controller controls an Island for as long as " +
        "you control this creature and this creature remains tapped."
    power = 0
    toughness = 1

    flags(AbilityFlag.MAY_NOT_UNTAP)

    stateTriggeredAbility {
        condition = Conditions.YouControl(
            GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
            negate = true,
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no Islands, sacrifice this creature."
    }

    activatedAbility {
        cost = Costs.Tap
        val t = target(
            "target creature whose controller controls an Island",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.controllerControls(
                        GameObjectFilter.Land.withSubtype(Subtype.ISLAND).youControl()
                    )
                )
            )
        )
        effect = GainControlEffect(
            t,
            Duration.WhileYouControlSourceAndSourceTapped("Seasinger")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5266aa1-e2ea-46b9-91ab-b94a7bb7e9f9.jpg?1783947910"
    }
}
