package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intruder Alarm — Stronghold #34 (canonical printing)
 * {2}{U} · Enchantment
 *
 * Creatures don't untap during their controllers' untap steps.
 * Whenever a creature enters, untap all creatures.
 *
 * The untap lock is Meekstone's [UntapLimitPerStep] with a limit of zero over every creature. The
 * trigger fires once per entering creature, whoever controls it.
 */
val IntruderAlarm = card("Intruder Alarm") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Creatures don't untap during their controllers' untap steps.\n" +
        "Whenever a creature enters, untap all creatures."

    staticAbility {
        ability = UntapLimitPerStep(GameObjectFilter.Creature, 0)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).enters()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Donato Giancola"
        flavorText = "Mirri was startled by Volrath's apparition—it had no scent."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e55f653-0952-4713-8d43-ca50acff9e3b.jpg?1783946569"
        ruling(
            "2023-09-01",
            "If multiple creatures enter the battlefield at one time, this ability triggers once for " +
                "each of those creatures."
        )
    }
}
