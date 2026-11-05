package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spellweaver Eternal — Hour of Devastation #46 (canonical printing)
 * {1}{U} · Creature — Zombie Snake Wizard · 2/1
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)
 *
 * Both keywords are keyword-only: the engine derives their triggers (see `Afflict`).
 */
val SpellweaverEternal = card("Spellweaver Eternal") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Zombie Snake Wizard"
    power = 2
    toughness = 1
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)"

    prowess()
    afflict(2)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Jason Felix"
        flavorText = "\"They were once our brothers and sisters.\"\n—Etha, former Rhet-crop initiate"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6bb7d1a-d606-46a8-a592-648c5d2e7382.jpg?1783936048"
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
