package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Recruitment Officer
 * {W}
 * Creature — Human Soldier
 * 2/1
 * {3}{W}: Look at the top four cards of your library. You may reveal a creature card with mana
 * value 3 or less from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 */
val RecruitmentOfficer = card("Recruitment Officer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "{3}{W}: Look at the top four cards of your library. You may reveal a creature card with mana value 3 or less from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Creature.manaValueAtMost(3),
            prompt = "You may reveal a creature card with mana value 3 or less from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Johan Grenier"
        flavorText = "\"Every name on this list is already a hero! Will you add your own to it?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c226656b-68d5-4df2-b313-a323a728c520.jpg?1783920125"
    }
}
