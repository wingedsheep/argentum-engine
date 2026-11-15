package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sibsig Muckdraggers
 * {8}{B}
 * Creature — Zombie
 * 3/6
 * Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)
 * When this creature enters, return target creature card from your graveyard to your hand.
 */
val SibsigMuckdraggers = card("Sibsig Muckdraggers") {
    manaCost = "{8}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 6
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "When this creature enters, return target creature card from your graveyard to your hand."

    keywords(Keyword.DELVE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creatureCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f88297c1-c612-47bf-80b7-374099b00e07.jpg"
    }
}
