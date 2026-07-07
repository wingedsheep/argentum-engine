package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Glorifier of Suffering
 * {2}{W}
 * Creature — Vampire Soldier
 * 3/2
 * Common, LCI #15, art by Lauren K. Cannon
 *
 * "When this creature enters, you may sacrifice another creature or artifact. When you do,
 *  put a +1/+1 counter on each of up to two target creatures."
 *
 * Implemented as an ETB [ReflexiveTriggerEffect]:
 *  - The action is a resolution-time choice (not a targeted selection at trigger-stack time):
 *    [SelectTargetEffect] prompts the controller to choose another creature or artifact they
 *    control, then [Effects.SacrificeTarget] sacrifices it. "Another" is enforced by the
 *    `.other()` filter which excludes the Glorifier itself.
 *  - `optional = true` — the whole action may be declined.
 *  - When the action completes the reflexive trigger goes on the stack. Its
 *    [reflexiveTargetRequirements] gather "up to two target creatures" (count = 2, optional = true
 *    → minCount = 0, so zero targets is legal).
 *  - [ForEachTargetEffect] then runs [Effects.AddCounters] once per selected creature so each
 *    chosen creature gets exactly one +1/+1 counter (not split between them).
 */
val GlorifierOfSuffering = card("Glorifier of Suffering") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Soldier"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may sacrifice another creature or artifact. When you do, put a +1/+1 counter on each of up to two target creatures."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(listOf(
                SelectTargetEffect(
                    requirement = TargetObject(
                        filter = TargetFilter.CreatureOrArtifact.youControl().other()
                    ),
                    storeAs = "toSacrifice"
                ),
                Effects.SacrificeTarget(EffectTarget.PipelineTarget("toSacrifice"))
            )),
            optional = true,
            reflexiveEffect = ForEachTargetEffect(
                listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
            ),
            reflexiveTargetRequirements = listOf(TargetCreature(count = 2, optional = true)),
            descriptionOverride = "You may sacrifice another creature or artifact. When you do, put a +1/+1 counter on each of up to two target creatures."
        )
        description = "When this creature enters, you may sacrifice another creature or artifact. When you do, put a +1/+1 counter on each of up to two target creatures."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Lauren K. Cannon"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7580ad36-7362-4dee-9511-d119173b70e8.jpg?1782694599"
    }
}
