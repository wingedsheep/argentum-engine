package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoyaltyAbilitiesCostAdditional

/**
 * Carth the Lion
 * {2}{B}{G}
 * Legendary Creature — Human Warrior
 * 3/5
 * Whenever Carth enters or a planeswalker you control dies, look at the top seven cards of your
 * library. You may reveal a planeswalker card from among them and put it into your hand. Put the
 * rest on the bottom of your library in a random order.
 * Planeswalkers' loyalty abilities you activate cost an additional [+1] to activate.
 *
 * The "enters or … dies" trigger is two triggered abilities sharing one effect. The surcharge is
 * [LoyaltyAbilitiesCostAdditional], totalled into the one loyalty payment: [+1] costs [+2], [0]
 * costs [+1], [−3] costs [−2] and can be activated with two loyalty.
 */
val CarthTheLion = card("Carth the Lion") {
    manaCost = "{2}{B}{G}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 5
    oracleText = "Whenever Carth enters or a planeswalker you control dies, look at the top seven cards " +
        "of your library. You may reveal a planeswalker card from among them and put it into your hand. " +
        "Put the rest on the bottom of your library in a random order.\n" +
        "Planeswalkers' loyalty abilities you activate cost an additional [+1] to activate."

    val dig = Patterns.Library.lookAtTopRevealMatchingToHand(
        count = 7,
        filter = GameObjectFilter.Planeswalker,
        prompt = "You may reveal a planeswalker card and put it into your hand",
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = dig
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Planeswalker.youControl()).dies()
        effect = dig
    }

    staticAbility {
        ability = LoyaltyAbilitiesCostAdditional(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e09e9b65-ff0e-4efe-b96a-ede69c96e2ea.jpg?1783926819"
    }
}
