package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guul Draz Overseer — Battle for Zendikar #112
 * {4}{B}{B} · Creature — Vampire · 3/4
 * Flying
 * Landfall — Whenever a land you control enters, other creatures you control get +1/+0 until end of
 * turn. If that land is a Swamp, those creatures get +2/+0 until end of turn instead.
 *
 * "If that land is a Swamp" reads the triggering land on resolution, the same
 * `EntityMatches(TriggeringEntity, …)` shape as Oran-Rief Hydra's Forest check; only one of the two
 * pumps is applied. The pump is Bolt Hound's "other creatures you control" group shape.
 */
val GuulDrazOverseer = card("Guul Draz Overseer") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Landfall — Whenever a land you control enters, other creatures you control get +1/+0 until end " +
        "of turn. If that land is a Swamp, those creatures get +2/+0 until end of turn instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val others = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Land.withSubtype("Swamp"),
            ),
            then = Patterns.Group.modifyStatsForAll(power = 2, toughness = 0, filter = others),
            otherwise = Patterns.Group.modifyStatsForAll(power = 1, toughness = 0, filter = others),
        )
        description = "Landfall — Whenever a land you control enters, other creatures you control get " +
            "+1/+0 until end of turn. If that land is a Swamp, those creatures get +2/+0 until end of " +
            "turn instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/793bb4e0-8fdd-440e-9e6e-92a899794cae.jpg?1783938201"
    }
}
