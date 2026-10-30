package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Renata, Called to the Hunt — Theros Beyond Death #196
 * {2}{G}{G} · Legendary Enchantment Creature — Demigod · * /3
 *
 * Renata's power is equal to your devotion to green.
 * Each other creature you control enters with an additional +1/+1 counter on it.
 *
 * The power is a characteristic-defining [dynamicPower] over [DynamicAmounts.devotionTo] (Zendikar
 * Incarnate's shape; toughness stays the printed 3). The second line is Bramblewood Paragon's
 * `otherOnly` [EntersWithCounters] replacement over every creature you control — a creature
 * entering at the same time as Renata gets nothing (ruling).
 */
val RenataCalledToTheHunt = card("Renata, Called to the Hunt") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment Creature — Demigod"
    toughness = 3
    oracleText = "Renata's power is equal to your devotion to green. (Each {G} in the mana costs of permanents " +
        "you control counts toward your devotion to green.)\n" +
        "Each other creature you control enters with an additional +1/+1 counter on it."

    dynamicPower(DynamicAmounts.devotionTo(Color.GREEN))

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b27e6b2-13ad-42b2-a121-70935913723d.jpg?1783931530"
        ruling("2020-01-24", "The ability that defines Renata's power works in all zones, not just the battlefield.")
        ruling(
            "2020-01-24",
            "If a creature you control would normally enter the battlefield with no +1/+1 counters on it, " +
                "Renata's second ability causes it to enter the battlefield with a +1/+1 counter on it."
        )
        ruling(
            "2020-01-24",
            "A creature entering the battlefield at the same time as Renata won't get an additional +1/+1 counter."
        )
    }
}
