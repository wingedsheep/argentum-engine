package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Epic Fight — Marvel Super Heroes #166
 * {2}{G} · Sorcery · Rare
 *
 * Choose one or both —
 * • Double target creature's power and toughness until end of turn.
 * • Target creature you control fights target creature an opponent controls.
 *
 * "Choose one or both" is `modal(chooseCount = 2, minChooseCount = 1)` — at least one mode, at
 * most both, with no extra cost. Modes are chosen and targeted at cast time (CR 601.2b) and
 * resolve in printed order, so doubling first and then fighting with the doubled creature is a
 * legal line when both modes are chosen.
 *
 * "Double its power and toughness" is the standard layer-7c +N/+N modification where N is the
 * creature's power / toughness read at resolution — Bulk Up's shape with both halves dynamic
 * instead of just power. The bonus is locked in when the effect is applied, so it does
 * not feed back on itself, and a creature with negative toughness is left alone rather than
 * halved-then-doubled. Each mode's targets are scoped to that mode, so
 * [EntityReference.Target]`(0)` reads *this* mode's creature.
 */
val EpicFight = card("Epic Fight") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Double target creature's power and toughness until end of turn.\n" +
        "• Target creature you control fights target creature an opponent controls."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Double target creature's power and toughness until end of turn") {
                val creature = target("target creature", Targets.Creature)
                effect = Effects.ModifyStats(
                    power = DynamicAmount.EntityProperty(
                        EntityReference.Target(0),
                        EntityNumericProperty.Power
                    ),
                    toughness = DynamicAmount.EntityProperty(
                        EntityReference.Target(0),
                        EntityNumericProperty.Toughness
                    ),
                    target = creature
                )
            }
            mode("Target creature you control fights target creature an opponent controls") {
                val yours = target(
                    "target creature you control",
                    TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
                )
                val theirs = target(
                    "target creature an opponent controls",
                    TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.opponentControls()))
                )
                effect = Effects.Fight(yours, theirs)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Wei Guan"
        flavorText = "\"You think being big is going to help you? Bub, all that does is make you " +
            "easier to hit.\"\n—Wolverine"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d521b80b-4b0b-4d69-bd95-4c83feaf2145.jpg?1783902918"
    }
}
