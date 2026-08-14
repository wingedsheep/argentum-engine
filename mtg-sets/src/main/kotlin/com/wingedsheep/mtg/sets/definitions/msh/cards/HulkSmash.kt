package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.dsl.teamworkModal
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * HULK SMASH! — Marvel Super Heroes #135
 * {1}{R} · Instant · Common
 *
 * Teamwork 4 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 4 or more.)
 * Choose one. If this spell was cast using teamwork, choose both instead.
 * • Destroy target noncreature artifact.
 * • Target creature you control deals damage equal to its power to target creature an opponent
 *   controls.
 *
 * The modal shape of teamwork — [teamworkModal] narrows the printed "choose both" to one mode
 * unless the teamwork cost was declared. CR 700.2 governs the mode count; the declaration it
 * branches on is made under CR 601.2b (*not* CR 702.194c, which is about targets).
 *
 * Mode 2 is the Rabid Bite shape — one-sided damage whose source is the attacking creature, so
 * deathtouch/lifelink on it apply and the victim deals nothing back. Its amount reads *this*
 * mode's first target ([EntityReference.Target]`(0)` is scoped to the mode's own target list), and
 * it is read at resolution, so a pump between cast and resolution counts.
 */
val HulkSmash = card("HULK SMASH!") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Teamwork 4 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 4 or more.)\n" +
        "Choose one. If this spell was cast using teamwork, choose both instead.\n" +
        "• Destroy target noncreature artifact.\n" +
        "• Target creature you control deals damage equal to its power to target creature an " +
        "opponent controls."

    teamwork(4)

    spell {
        teamworkModal {
            mode("Destroy target noncreature artifact") {
                val artifact = target(
                    "target noncreature artifact",
                    TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.notCreature())),
                )
                effect = Effects.Destroy(artifact)
            }
            mode(
                "Target creature you control deals damage equal to its power to target creature " +
                    "an opponent controls",
            ) {
                val yours = target("target creature you control", Targets.CreatureYouControl)
                val theirs = target(
                    "target creature an opponent controls",
                    Targets.CreatureOpponentControls,
                )
                effect = Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(
                        EntityReference.Target(0),
                        EntityNumericProperty.Power,
                    ),
                    target = theirs,
                    damageSource = yours,
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/374ffb3e-0753-4682-936a-ae6921ace475.jpg?1783902929"
    }
}
