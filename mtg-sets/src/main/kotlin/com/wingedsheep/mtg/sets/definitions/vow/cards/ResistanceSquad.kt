package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect

/**
 * Resistance Squad
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 * When this creature enters, if you control another Human, draw a card.
 */
val ResistanceSquad = card("Resistance Squad") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "When this creature enters, if you control another Human, draw a card."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.YouControl(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
            excludeSelf = true
        )
        effect = DrawCardsEffect(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Joshua Raphael"
        flavorText = "\"It's not that I didn't expect some defiance, but I did hope it wouldn't be so heavily armed.\"\n—Olivia Voldaren"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/093ae747-350f-4253-b903-8c6892d78c83.jpg?1782703173"
    }
}
