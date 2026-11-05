package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Manticore Eternal — Hour of Devastation #103 (canonical printing)
 * {3}{R}{R} · Creature — Zombie Manticore · 5/4
 *
 * Afflict 3 (Whenever this creature becomes blocked, defending player loses 3 life.)
 * This creature attacks each combat if able.
 *
 * Afflict is keyword-only: the engine derives the becomes-blocked trigger (see `Afflict`).
 */
val ManticoreEternal = card("Manticore Eternal") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Zombie Manticore"
    power = 5
    toughness = 4
    oracleText = "Afflict 3 (Whenever this creature becomes blocked, defending player loses 3 life.)\nThis creature attacks each combat if able."

    afflict(3)

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Yeong-Hao Han"
        flavorText = "While most Eternals were initiates who completed the trials, Bolas had plans for some of Amonkhet's other denizens."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b03a9686-d30b-4048-8b0a-23fd2f0aed4b.jpg?1783936025"
        ruling("2017-07-14", "If, during your declare attackers step, Manticore Eternal is tapped or is affected by a spell or ability that says it can't attack, then it doesn't attack. If there's a cost associated with having it attack, you aren't forced to pay that cost, so it doesn't have to attack in that case either.")
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
