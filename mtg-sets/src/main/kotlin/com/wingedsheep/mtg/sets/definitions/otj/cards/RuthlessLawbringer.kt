package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ruthless Lawbringer
 * {1}{W}{B}
 * Creature — Vampire Assassin
 * 3/2
 *
 * When this creature enters, you may sacrifice another creature. When you do,
 * destroy target nonland permanent.
 *
 * "Sacrifice another creature" is a resolution-time choice, not a target: the
 * player accepts the optional first, then chooses which creature (so declining
 * never forces a commitment). The "When you do" reflexive trigger then targets a
 * nonland permanent, chosen as that second ability goes on the stack.
 */
val RuthlessLawbringer = card("Ruthless Lawbringer") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire Assassin"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may sacrifice another creature. When you do, destroy target nonland permanent."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(listOf(
                SelectTargetEffect(
                    requirement = TargetObject(
                        filter = TargetFilter.CreatureYouControl.other()
                    ),
                    storeAs = "creatureToSacrifice"
                ),
                Effects.SacrificeTarget(EffectTarget.PipelineTarget("creatureToSacrifice"))
            )),
            optional = true,
            reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(Targets.NonlandPermanent),
            descriptionOverride = "You may sacrifice another creature. When you do, destroy target nonland permanent."
        )
        description = "When this creature enters, you may sacrifice another creature. When you do, destroy target nonland permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Joshua Raphael"
        flavorText = "There's only one law that truly matters to the Sterling Company: never stand in the way of profit."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/927b5498-23f1-47c0-b441-7daaeb54f9b8.jpg?1712356201"
    }
}
