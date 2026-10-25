package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Intrepid Outlander
 * {1}{G}
 * Creature — Orc Ranger
 * 2/3
 * Reach
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, venture into the dungeon. (Enter the first room or advance to the next room.)
 *
 * Pack tactics is the attack trigger gated by [AttackPredicate.AttackersTotalPowerAtLeast], as on
 * Gnoll Hunter.
 */
val IntrepidOutlander = card("Intrepid Outlander") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Orc Ranger"
    oracleText = "Reach\nPack tactics — Whenever this creature attacks, if you attacked with creatures with " +
        "total power 6 or greater this combat, venture into the dungeon. " +
        "(Enter the first room or advance to the next room.)"
    power = 2
    toughness = 3

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30b00777-d921-4264-9faf-5c048b656210.jpg?1783926460"
    }
}
