package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phylactery Lich
 * {B}{B}{B}
 * Creature — Zombie
 * 5/5
 * Indestructible
 * As this creature enters, put a phylactery counter on an artifact you control.
 * When you control no permanents with phylactery counters on them, sacrifice this creature.
 *
 * The counter is an [EntersWithChoice] replacement, not a target: the artifact is chosen while the
 * Lich is entering, from artifacts already on the battlefield (one entering alongside it is never a
 * candidate), and the choice is mandatory. With no artifact the choice has no candidates and nothing
 * happens. The sacrifice is a state trigger over *any* permanent you control with a phylactery
 * counter, not just the one this Lich marked.
 */
val PhylacteryLich = card("Phylactery Lich") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 5
    toughness = 5
    oracleText = "Indestructible\n" +
        "As this creature enters, put a phylactery counter on an artifact you control.\n" +
        "When you control no permanents with phylactery counters on them, sacrifice this creature."

    keywords(Keyword.INDESTRUCTIBLE)

    replacementEffect(
        EntersWithChoice(
            ChoiceType.CREATURE_ON_BATTLEFIELD,
            permanentFilter = GameObjectFilter.Artifact.youControl(),
            then = Effects.Pipeline(descriptionOverride = "Put a phylactery counter on it") {
                run(Effects.AddCounters(CounterType.PHYLACTERY, 1, EffectTarget.ChosenCreature))
            },
        )
    )

    stateTriggeredAbility {
        condition = Conditions.YouControl(
            GameObjectFilter.Permanent.withCounter(CounterType.PHYLACTERY),
            negate = true,
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no permanents with phylactery counters on them, sacrifice this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d088983-92c1-4f4d-8abf-dd20347495b5.jpg?1783941813"
        ruling(
            "2018-07-13",
            "Phylactery Lich's first ability doesn't target the artifact. No player may take actions between the " +
                "time you choose an artifact and the time you put a phylactery counter on it."
        )
        ruling(
            "2018-07-13",
            "If you control no artifacts as Phylactery Lich enters the battlefield, its first ability won't do " +
                "anything. As soon as it enters the battlefield, its last ability will trigger (unless you control " +
                "some other permanent with a phylactery counter on it) and you'll have to sacrifice it."
        )
        ruling(
            "2018-07-13",
            "If Phylactery Lich and an artifact are entering the battlefield under your control at the same time, " +
                "you can't put a phylactery counter on that artifact. You must choose an artifact you control " +
                "that's already on the battlefield."
        )
        ruling(
            "2018-07-13",
            "Phylactery Lich's last ability is a \"state trigger.\" Once a state trigger triggers, it won't trigger " +
                "again as long as the ability is on the stack. If the ability is countered and the trigger " +
                "condition is still true, it will immediately trigger again."
        )
        ruling(
            "2018-07-13",
            "Phylactery Lich's last ability checks your permanents for any phylactery counters, not just the " +
                "specific one that it caused you to put on an artifact. For example, if you control two Phylactery " +
                "Liches and two different artifacts with phylactery counters, neither Phylactery Lich will be " +
                "sacrificed if one of those artifacts leaves the battlefield."
        )
    }
}
