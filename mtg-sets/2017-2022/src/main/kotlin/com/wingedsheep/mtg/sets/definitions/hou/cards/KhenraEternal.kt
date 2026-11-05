package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Khenra Eternal — Hour of Devastation #66 (canonical printing)
 * {1}{B} · Creature — Zombie Jackal Warrior · 2/2
 *
 * Afflict 1 (Whenever this creature becomes blocked, defending player loses 1 life.)
 *
 * Afflict is keyword-only: the engine derives the becomes-blocked trigger (see `Afflict`).
 */
val KhenraEternal = card("Khenra Eternal") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie Jackal Warrior"
    power = 2
    toughness = 2
    oracleText = "Afflict 1 (Whenever this creature becomes blocked, defending player loses 1 life.)"

    afflict(1)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Tomasz Jedruszek"
        flavorText = "For many years, servant mummies toiled in the mines to extract lazotep. The emergence of the Eternals revealed the sacred substance's true purpose."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abbdc277-4a76-44a9-aeda-edabab1f579e.jpg?1783936040"
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
