package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Avatar of the Resolute
 * {G}{G}
 * Creature — Avatar
 * 3/2
 * Reach, trample
 * This creature enters with a +1/+1 counter on it for each other creature you control with a
 * +1/+1 counter on it.
 *
 * The count is `notSourceItself` for "each **other** creature" — the Avatar is measured against a
 * view that already holds it. Creatures entering at the same time are left out by the engine's
 * entry view (simultaneous entrants are excluded while enters-with counts are evaluated), which is
 * the printed ruling.
 */
val AvatarOfTheResolute = card("Avatar of the Resolute") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Avatar"
    power = 3
    toughness = 2
    oracleText = "Reach, trample\n" +
        "This creature enters with a +1/+1 counter on it for each other creature you control " +
        "with a +1/+1 counter on it."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.count(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Creature
                    .withCounter(CounterType.PLUS_ONE_PLUS_ONE)
                    .notSourceItself()
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Jeff Simpson"
        flavorText = "It exemplifies the ideals of the Dromoka: strength, unity, and honor."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf18a2f-f93e-4a18-92d5-c9d2cca276d7.jpg?1783938582"
        ruling("2015-02-25", "When determining how many +1/+1 counters Avatar of the Resolute enters the battlefield with, don't count any creatures entering the battlefield at the same time, even if those creatures enter the battlefield with +1/+1 counters on them.")
    }
}
