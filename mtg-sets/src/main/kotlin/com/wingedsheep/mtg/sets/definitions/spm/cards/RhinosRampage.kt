package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rhino's Rampage — {R/G}
 * Sorcery
 *
 * Target creature you control gets +1/+0 until end of turn. It fights target creature an opponent
 * controls. When excess damage is dealt to the creature an opponent controls this way, destroy up
 * to one target noncreature artifact with mana value 3 or less.
 *
 * Modeling notes:
 *  - A single cast-time "target creature you control" ([yourCreature]) is both the +1/+0 recipient
 *    ([Effects.ModifyStats], default end-of-turn) and the fight's first participant, matching
 *    "Target creature ... gets +1/+0 ... It fights ...". The pump runs first so its +1 power is in
 *    the projected state the fight reads. "target creature an opponent controls" is the second,
 *    independent cast-time target.
 *  - The fight ([Effects.Fight]) stores the excess damage (CR 120.4a) it deals **to the opponent's
 *    creature** into the pipeline number `excess` via `excessDamageVariable` — the same capture The
 *    Last Agni Kai uses. `excess` stays 0 when the fought creature is dealt only lethal/sub-lethal
 *    damage (or has died to another effect first).
 *  - The reflexive "When excess damage is dealt ... this way, destroy up to one target ..." is a
 *    [GatedEffect] with a [Gate.WhenCondition] on `excess >= 1` — a synchronous state test that
 *    reads the fight's stored number, so the destroy branch is entered **only** when excess damage
 *    was actually dealt (never otherwise; the artifact is not touched on a plain lethal/whiff). The
 *    "up to one target" optionality is a [MayEffect] wrapping a resolution-time
 *    [Effects.SelectTarget] + [Effects.Destroy]: gating outside the targeting means no artifact
 *    target is offered at all unless excess occurred, and the player may still decline to destroy
 *    anything ("up to one"). "noncreature artifact with mana value 3 or less" is
 *    `GameObjectFilter.Artifact.notCreature().manaValueAtMost(3)`.
 */
val RhinosRampage = card("Rhino's Rampage") {
    manaCost = "{R/G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+0 until end of turn. It fights target " +
        "creature an opponent controls. When excess damage is dealt to the creature an opponent " +
        "controls this way, destroy up to one target noncreature artifact with mana value 3 or less."

    spell {
        val yourCreature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl)
        )
        val theirCreature = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls)
        )

        effect = Effects.ModifyStats(1, 0, yourCreature)
            .then(Effects.Fight(yourCreature, theirCreature, excessDamageVariable = "excess"))
            .then(
                GatedEffect(
                    gate = Gate.WhenCondition(
                        Conditions.CompareAmounts(
                            DynamicAmount.VariableReference("excess"),
                            ComparisonOperator.GTE,
                            DynamicAmount.Fixed(1),
                        )
                    ),
                    then = MayEffect(
                        effect = Effects.SelectTarget(
                            TargetPermanent(
                                filter = TargetFilter(
                                    GameObjectFilter.Artifact.notCreature().manaValueAtMost(3)
                                )
                            ),
                            storeAs = "rampageArtifact",
                        ).then(Effects.Destroy(EffectTarget.PipelineTarget("rampageArtifact"))),
                        descriptionOverride = "destroy up to one target noncreature artifact " +
                            "with mana value 3 or less",
                    ),
                    descriptionOverride = "When excess damage is dealt to the creature an opponent " +
                        "controls this way, destroy up to one target noncreature artifact with " +
                        "mana value 3 or less.",
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Nino Is"
        flavorText = "Spider-Man wasn't looking to fight, but that was all Rhino wanted."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f668817c-1cab-44c5-b6a8-95113e480d5e.jpg?1783905314"
    }
}
