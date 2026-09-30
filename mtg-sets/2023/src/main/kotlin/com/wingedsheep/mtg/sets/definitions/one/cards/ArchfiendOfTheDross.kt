package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archfiend of the Dross
 * {2}{B}{B}
 * Creature — Phyrexian Demon
 * 6/6
 *
 * Flying
 * This creature enters with four oil counters on it.
 * At the beginning of your upkeep, remove an oil counter from this creature. Then if it has no oil
 * counters on it, you lose the game.
 * Whenever a creature an opponent controls dies, its controller loses 2 life.
 *
 * The "then if" is checked on resolution, after the removal. If the Archfiend has left the
 * battlefield by then, nothing is removed and the check reads the oil counters it last had there
 * (ruling 2023-02-04) — the `Self` counter read falls back to its battlefield-exit snapshot.
 */
val ArchfiendOfTheDross = card("Archfiend of the Dross") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "This creature enters with four oil counters on it.\n" +
        "At the beginning of your upkeep, remove an oil counter from this creature. Then if it has " +
        "no oil counters on it, you lose the game.\n" +
        "Whenever a creature an opponent controls dies, its controller loses 2 life."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 4, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.RemoveCounters(CounterType.OIL, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtMost(CounterType.OIL, 0),
                then = Effects.LoseGame(message = "Archfiend of the Dross"),
            )
        description = "At the beginning of your upkeep, remove an oil counter from this creature. " +
            "Then if it has no oil counters on it, you lose the game."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.LoseLife(2, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86b0edaf-8cfd-4508-9554-6f0fddc2dfc4.jpg?1783918050"

        ruling(
            "2023-02-04",
            "If Archfiend of the Dross somehow loses all of its oil counters due to an effect other " +
                "than its upkeep triggered ability, this won't cause you to lose the game until the " +
                "next time that ability resolves."
        )
        ruling(
            "2023-02-04",
            "If Archfiend of the Dross is no longer on the battlefield as its upkeep triggered " +
                "ability resolves, use the number of oil counters it had the last time it existed on " +
                "the battlefield to determine whether you lose the game. You can't remove oil counters " +
                "from it if it's not on the battlefield, so you won't lose the game if it had only one " +
                "oil counter on it before leaving the battlefield."
        )
    }
}
