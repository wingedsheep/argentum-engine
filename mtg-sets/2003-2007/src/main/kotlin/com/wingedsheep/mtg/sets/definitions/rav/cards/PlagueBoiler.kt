package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Plague Boiler — Ravnica: City of Guilds #269
 * {3} · Artifact · Rare
 *
 * At the beginning of your upkeep, put a plague counter on this artifact.
 * {1}{B}{G}: Put a plague counter on this artifact or remove a plague counter from it.
 * When this artifact has three or more plague counters on it, sacrifice it. If you do, destroy all
 * nonland permanents.
 *
 * A three-turn fuse anyone can hurry along and only its controller can hold back — the middle
 * ability is the whole card, letting you spend {1}{B}{G} to detonate a turn early or to keep the
 * board alive one more turn.
 *
 * **The middle ability is a resolution-time choice, not a cast-time modal.** The printed text has
 * no "Choose one —" bulleted list, so under CR 700.2 it is not a modal spell/ability at all: the
 * player picks add-or-remove *as it resolves*, which is what the card's first ruling says. That
 * maps onto [ModalEffect.chooseOne] on an *activated* ability, whose modes the `ModalEffectExecutor`
 * presents at resolution rather than at activation.
 *
 * **The third ability is genuinely state-triggered** (CR 603.8) and not a state-based action: it
 * fires on the false → true transition of "three or more plague counters", goes on the stack, and
 * does its work on resolution. Both halves of that matter for the second ruling — removing a
 * counter in response does *not* stop the sacrifice (the condition is not rechecked on resolution),
 * but getting the Boiler off the battlefield does, because there is then nothing to sacrifice and
 * [Effects.IfYouDo] gates the wipe on the sacrifice actually happening.
 *
 * The plague counter ([CounterType.PLAGUE]) is a passive storage counter with no inherent rule.
 *
 * One deliberate simplification: with no plague counters on the Boiler, the "remove" mode is still
 * offered and simply does nothing, where the printed card would not let you choose it. Nothing
 * downstream reads the choice, so the two differ only in which dead button the UI shows.
 */
val PlagueBoiler = card("Plague Boiler") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, put a plague counter on this artifact.\n" +
        "{1}{B}{G}: Put a plague counter on this artifact or remove a plague counter from it.\n" +
        "When this artifact has three or more plague counters on it, sacrifice it. If you do, " +
        "destroy all nonland permanents."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.PLAGUE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a plague counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}{G}")
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Put a plague counter on this artifact",
                effect = Effects.AddCounters(CounterType.PLAGUE, 1, EffectTarget.Self),
            ),
            EffectChoice(
                label = "Remove a plague counter from this artifact",
                effect = Effects.RemoveCounters(CounterType.PLAGUE, 1, EffectTarget.Self),
            )
        ))
        description = "{1}{B}{G}: Put a plague counter on this artifact or remove a plague " +
            "counter from it."
    }

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PLAGUE, 3)
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            then = Effects.DestroyAll(GameObjectFilter.NonlandPermanent),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "When this artifact has three or more plague counters on it, sacrifice it. " +
            "If you do, destroy all nonland permanents"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "269"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/889bab2c-d383-4a04-9f93-d507ba1973d9.jpg?1783943595"
        ruling(
            "2005-10-01",
            "You choose whether to add or remove a counter when the second ability resolves. You " +
                "can't choose to remove a counter if there isn't one there."
        )
        ruling(
            "2005-10-01",
            "If the third ability triggers, removing a counter in response won't stop the effect. " +
                "However, somehow removing Plague Boiler from the battlefield in response would " +
                "stop the effect because then you wouldn't be able to sacrifice Plague Boiler."
        )
        ruling("2005-10-01", "The sacrifice is done on resolution of the triggered ability.")
    }
}
