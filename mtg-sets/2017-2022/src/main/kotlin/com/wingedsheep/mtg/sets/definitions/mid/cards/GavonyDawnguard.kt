package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Gavony Dawnguard
 * {1}{W}{W}
 * Creature — Human Soldier
 * 3/3
 * Ward {1}
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, look at the top four cards of your library.
 * You may reveal a creature card with mana value 3 or less from among them and put it into your
 * hand. Put the rest on the bottom of your library in any order.
 */
val GavonyDawnguard = card("Gavony Dawnguard") {
    manaCost = "{1}{W}{W}"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Ward {1}\n" +
        "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, look at the top four cards of your library. " +
        "You may reveal a creature card with mana value 3 or less from among them and put it into your hand. " +
        "Put the rest on the bottom of your library in any order."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Creature.manaValueAtMost(3),
            prompt = "You may reveal a creature card with mana value 3 or less from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Micah Epstein"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58891dae-1c59-4f53-82ec-5b4c47133182.jpg?1783925658"
    }
}
