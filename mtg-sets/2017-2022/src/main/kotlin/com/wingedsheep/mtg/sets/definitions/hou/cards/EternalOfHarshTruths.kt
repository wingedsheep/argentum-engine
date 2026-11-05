package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eternal of Harsh Truths — Hour of Devastation #34 (canonical printing)
 * {2}{U} · Creature — Zombie Cleric · 1/3
 *
 * Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)
 * Whenever this creature attacks and isn't blocked, draw a card.
 *
 * Afflict is keyword-only: the engine derives the becomes-blocked trigger (see `Afflict`).
 */
val EternalOfHarshTruths = card("Eternal of Harsh Truths") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Zombie Cleric"
    power = 1
    toughness = 3
    oracleText = "Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)\n" +
        "Whenever this creature attacks and isn't blocked, draw a card."

    afflict(2)

    triggeredAbility {
        trigger = Triggers.self.attacksAndIsntBlocked()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Izzy"
        flavorText = "\"If creating the Eternals was Bolas's plan here, what is his plan for the Eternals?\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0389daf7-0818-488f-8e6a-94818a6c94a6.jpg?1783936052"
        ruling("2017-07-14", "An ability that triggers when something \"attacks and isn't blocked\" triggers in the declare blockers step after blockers are declared if (1) that creature is attacking and (2) no creatures are declared to block it. It will trigger even if that creature was put onto the battlefield attacking rather than having been declared as an attacker in the declare attackers step.")
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
