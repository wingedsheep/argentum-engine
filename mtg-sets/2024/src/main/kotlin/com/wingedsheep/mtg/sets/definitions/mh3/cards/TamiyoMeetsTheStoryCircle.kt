package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tamiyo Meets the Story Circle
 * {1}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Until your next turn, whenever a creature attacks you or a planeswalker you control, it gets
 *     -2/-0 until end of turn.
 * II — Discard any number of cards, then investigate twice for each card discarded this way.
 * III — Shuffle up to three target cards from your graveyard into your library.
 *
 * - I is Jace, Reality Sculptor's −3 shape: a filter-scoped event delayed trigger per attacker that
 *   lives until your next turn ([DelayedTriggerExpiry.UntilControllersNextTurn]). It belongs to the
 *   chapter ability, so it keeps working after the Saga is sacrificed.
 * - II reads the discarded set's size and investigates twice that many times — sequential
 *   investigates (per the 2024-06-07 ruling), each a separate investigate event.
 * - III is The Bath Song's graveyard shuffle, capped at three targets and allowed to choose none.
 */
val TamiyoMeetsTheStoryCircle = card("Tamiyo Meets the Story Circle") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Until your next turn, whenever a creature attacks you or a planeswalker you control, it gets -2/-0 until end of turn.\n" +
        "II — Discard any number of cards, then investigate twice for each card discarded this way.\n" +
        "III — Shuffle up to three target cards from your graveyard into your library."

    sagaChapter(1) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks(),
            effect = Effects.ModifyStats(-2, 0, EffectTarget.TriggeringEntity),
            expiry = DelayedTriggerExpiry.UntilControllersNextTurn,
        )
    }

    sagaChapter(2) {
        effect = Effects.Pipeline {
            val discarded = runStoringCollection { Patterns.Hand.discardAnyNumber(storeAs = it) }
            run(Effects.Investigate(discarded.count * 2))
        }
    }

    sagaChapter(3) {
        targets(
            TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD),
            count = 3,
            optional = true,
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY)
        ) then Effects.ShuffleLibrary()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee66a06e-a461-46af-a318-550bc35de5d0.jpg?1783911287"
        ruling(
            "2024-06-07",
            "If you're instructed to investigate multiple times, those actions are sequential, meaning you'll create that many Clue tokens one at a time."
        )
    }
}
