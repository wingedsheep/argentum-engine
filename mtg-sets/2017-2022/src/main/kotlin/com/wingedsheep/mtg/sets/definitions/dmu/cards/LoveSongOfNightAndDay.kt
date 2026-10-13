package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val BIRD_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/5/f/5f3034f6-145f-4e60-9e55-c4054fd8e70f.jpg?1783921132"

/**
 * Love Song of Night and Day
 * {2}{W}
 * Enchantment — Saga
 *
 * Read ahead
 * I — You and target opponent each draw two cards.
 * II — Create a 1/1 white Bird creature token with flying.
 * III — Put a +1/+1 counter on each of up to two target creatures.
 *
 * Chapter I's opponent is its only target, so if it becomes illegal the ability fizzles and
 * you draw nothing either (per the 2022-09-09 ruling).
 */
val LoveSongOfNightAndDay = card("Love Song of Night and Day") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — You and target opponent each draw two cards.\n" +
        "II — Create a 1/1 white Bird creature token with flying.\n" +
        "III — Put a +1/+1 counter on each of up to two target creatures."

    readAhead()

    sagaChapter(1) {
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(2) then Effects.DrawCards(2, opponent)
    }

    sagaChapter(2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = BIRD_TOKEN_IMAGE
        )
    }

    sagaChapter(3) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e08d288-2f09-4856-a9ed-cc8aa8d953d7.jpg?1783921363"

        ruling("2022-09-09", "If Love Song of Night and Day's first chapter ability can't legally target an opponent, or if the ability's target becomes illegal while the ability is on the stack, you don't draw any cards.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
