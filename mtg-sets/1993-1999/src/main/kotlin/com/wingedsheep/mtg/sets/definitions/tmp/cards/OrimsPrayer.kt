package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Orim's Prayer
 * {1}{W}{W}
 * Enchantment
 * Whenever one or more creatures attack you, you gain 1 life for each attacking creature.
 *
 * The 2008-04-01 ruling clarifies that the ability only triggers when creatures attack the
 * player; creatures attacking a planeswalker the player controls do not cause it to trigger.
 */
val OrimsPrayer = card("Orim's Prayer") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures attack you, you gain 1 life for each attacking creature."

    triggeredAbility {
        trigger = Triggers.CreaturesAttackYou
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dc45565-4b56-49ba-b115-be8e0de7d937.jpg?1562053272"
        flavorText = "\"As usual, there will be time for prayer only *after* the worst happens.\"\n—Orim, Samite healer"
        ruling("2008-04-01", "The ability only triggers when one or more creatures attack you. It will not trigger on creatures attacking a planeswalker you control.")
    }
}
