package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mister Hyde, Monster Within (MSH #176) — {2}{G} Legendary Creature — Human Villain · 2/2
 *
 * At the beginning of your upkeep, choose one —
 * • Put a +1/+1 counter on Mister Hyde.
 * • Remove a counter from a creature you control. If you do, draw a card.
 *
 * A modal *triggered* ability (CR 603.3c) on [Triggers.YourUpkeep], built as a
 * [ModalEffect.chooseOne] the way White Widow, Free Agent builds its enters trigger. Neither mode
 * targets, so both the mode and the creature are chosen without targeting — the mode as the
 * ability goes on the stack (CR 601.2b), the creature on resolution.
 *
 * Mode 2's creature is therefore picked mid-resolution with [Effects.SelectTarget] into a pipeline
 * slot (the Agatha's Soul Cauldron shape) rather than declared as a target, and the draw hangs off
 * [Effects.IfYouDo] with [SuccessCriterion.CountersRemoved] — counter removal is not a zone move, so
 * `Auto` can't infer it and a creature with no counters must not draw. Any kind of counter counts,
 * not just +1/+1.
 *
 * [Effects.RemoveCountersUpTo]`(1, …)` is the removal primitive: the controller picks which kind
 * to take off, capped at one counter total. It also permits taking off zero, which the printed
 * "Remove a counter" doesn't — a harmless liberty, since the mode is already declinable by
 * pointing it at a creature with no counters, and the "if you do" gate keeps the draw honest
 * either way.
 */
val MisterHydeMonsterWithin = card("Mister Hyde, Monster Within") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Villain"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, choose one —\n" +
        "• Put a +1/+1 counter on Mister Hyde.\n" +
        "• Remove a counter from a creature you control. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on Mister Hyde",
            ),
            Mode.noTarget(
                Effects.Composite(
                    Effects.SelectTarget(Targets.CreatureYouControl, storeAs = "hydeCounterSource"),
                    Effects.IfYouDo(
                        action = Effects.RemoveCountersUpTo(
                            1,
                            EffectTarget.PipelineTarget("hydeCounterSource", 0),
                        ),
                        ifYouDo = Effects.DrawCards(1),
                        successCriterion = SuccessCriterion.CountersRemoved,
                    ),
                ),
                "Remove a counter from a creature you control. If you do, draw a card",
            ),
        )
        description = "At the beginning of your upkeep, choose one — " +
            "• Put a +1/+1 counter on Mister Hyde. " +
            "• Remove a counter from a creature you control. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Svetlin Velinov"
        flavorText = "\"I read what I wanted to read and became what I wanted to become.\"\n—Dr. Calvin Zabo"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cf30476-ed92-4dc2-86b7-2b5fd60a2de7.jpg?1783902916"
    }
}
