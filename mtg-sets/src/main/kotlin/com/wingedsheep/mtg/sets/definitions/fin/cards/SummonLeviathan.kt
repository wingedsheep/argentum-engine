package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Summon: Leviathan
 * {4}{U}{U}
 * Enchantment Creature — Saga Leviathan
 * 6/6
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Return each creature that isn't a Kraken, Leviathan, Merfolk, Octopus, or Serpent to its
 *     owner's hand.
 * II, III — Until end of turn, whenever a Kraken, Leviathan, Merfolk, Octopus, or Serpent attacks,
 *     draw a card.
 * Ward {2}
 *
 * Chapter I is a filtered mass-return: gather every battlefield creature whose subtype is none of
 * the five "sea" types and bounce them (the Sunderflock template). Leviathan itself carries the
 * Leviathan subtype, so the filter excludes it without needing `excludeSelf`.
 *
 * Chapters II and III each install an end-of-turn delayed triggered ability that fires *every* time
 * (`fireOnce = false`) a sea creature attacks — regardless of controller, matching "whenever a
 * [type] attacks" — and draws the Saga controller a card. Each chapter creates its own copy, so on
 * the turn chapter II resolves and again on the turn chapter III resolves there is a fresh
 * until-end-of-turn watcher.
 */
val SummonLeviathan = card("Summon: Leviathan") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Saga Leviathan"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Return each creature that isn't a Kraken, Leviathan, Merfolk, Octopus, or Serpent to its owner's hand.\n" +
        "II, III — Until end of turn, whenever a Kraken, Leviathan, Merfolk, Octopus, or Serpent attacks, draw a card.\n" +
        "Ward {2}"
    power = 6
    toughness = 6

    keywordAbility(KeywordAbility.ward("{2}"))

    // "Return each creature that isn't a Kraken, Leviathan, Merfolk, Octopus, or Serpent."
    sagaChapter(1) {
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(
                GameObjectFilter.Creature
                    .notSubtype(Subtype.KRAKEN)
                    .notSubtype(Subtype.LEVIATHAN)
                    .notSubtype(Subtype.MERFOLK)
                    .notSubtype(Subtype.OCTOPUS)
                    .notSubtype(Subtype.SERPENT),
            ),
        )
    }

    sagaChapter(2) { effect = seaCreatureAttackDraw() }
    sagaChapter(3) { effect = seaCreatureAttackDraw() }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "OTUMAMI"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7f26a9-b203-4ee7-88f1-3d9c77a25bcb.jpg?1782686537"
    }
}

/**
 * "Until end of turn, whenever a Kraken, Leviathan, Merfolk, Octopus, or Serpent attacks, draw a
 * card." A repeating (`fireOnce = false`) end-of-turn delayed trigger watching attack declarations
 * by any sea creature. A fresh instance is built per chapter so II and III spawn independent
 * watchers.
 */
private fun seaCreatureAttackDraw(): Effect = CreateDelayedTriggerEffect(
    trigger = TriggerSpec(
        event = EventPattern.AttackEvent(
            filter = GameObjectFilter.Creature.withAnySubtype(
                "Kraken", "Leviathan", "Merfolk", "Octopus", "Serpent",
            ),
        ),
    ),
    fireOnce = false,
    expiry = DelayedTriggerExpiry.EndOfTurn,
    effect = Effects.DrawCards(1),
)
