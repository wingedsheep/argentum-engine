package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Door of Destinies — Morningtide #143
 * {4} · Artifact
 *
 * As this artifact enters, choose a creature type.
 * Whenever you cast a spell of the chosen type, put a charge counter on this artifact.
 * Creatures you control of the chosen type get +1/+1 for each charge counter on this artifact.
 *
 * "A spell of the chosen type" is any spell (creature or Kindred) carrying the subtype, so the
 * cast trigger filters `GameObjectFilter.Any` rather than creatures only. The trigger resolves
 * before the spell, so a creature of the chosen type enters with the new counter already counted.
 * The anthem is a live static sized by the charge counters currently on the Door.
 */
val DoorOfDestinies = card("Door of Destinies") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\n" +
        "Whenever you cast a spell of the chosen type, put a charge counter on this artifact.\n" +
        "Creatures you control of the chosen type get +1/+1 for each charge counter on this artifact."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withChosenSubtype())
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    staticAbility {
        val charge = DynamicAmounts.countersOnSelf(CounterType.CHARGE)
        ability = GrantDynamicStats(
            filter = GroupFilter.ChosenSubtypeCreatures().youControl(),
            powerBonus = charge,
            toughnessBonus = charge
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Larry MacDougall"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac4800be-5f77-42f5-914c-2a8e647e3af5.jpg?1783942774"
        ruling(
            "2008-04-01",
            "If you cast a creature spell of the chosen type, Door of Destinies will get a charge counter before the creature enters. The creature will enter with the additional boost to its power and toughness."
        )
        ruling(
            "2013-07-01",
            "Creature types, such as Human or Sliver, appear after the dash on the type line of creatures. Notably, artifact is not a creature type. A creature may have more than one creature type, such as Goblin Warrior. Such a creature would benefit from Door of Destinies if the chosen creature type was Goblin or Warrior."
        )
    }
}
