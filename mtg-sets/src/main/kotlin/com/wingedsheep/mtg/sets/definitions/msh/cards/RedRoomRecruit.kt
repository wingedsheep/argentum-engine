package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Red Room Recruit
 * {1}{B}
 * Creature — Human Spy Villain
 * 1/2
 * When this creature enters, it connives. (Draw a card, then discard a card. If you discarded a
 * nonland card, put a +1/+1 counter on this creature.)
 */
val RedRoomRecruit = card("Red Room Recruit") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Spy Villain"
    oracleText = "When this creature enters, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Borja Pindado"
        flavorText = "\"The Red Room doesn't grade on a curve. Only the deadliest make it out.\"\n—Black Widow, Natasha Romanoff"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f52a5ba3-a618-4f52-9d33-ba85345eb627.jpg?1783902938"
    }
}
