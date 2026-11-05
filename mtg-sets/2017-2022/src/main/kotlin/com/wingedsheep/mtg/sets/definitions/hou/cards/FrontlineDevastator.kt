package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frontline Devastator — Hour of Devastation #93 (canonical printing)
 * {3}{R} · Creature — Zombie Minotaur Warrior · 3/3
 *
 * Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)
 * {1}{R}: This creature gets +1/+0 until end of turn.
 *
 * Afflict is keyword-only: the engine derives the becomes-blocked trigger (see `Afflict`).
 */
val FrontlineDevastator = card("Frontline Devastator") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Zombie Minotaur Warrior"
    power = 3
    toughness = 3
    oracleText = "Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)\n{1}{R}: This creature gets +1/+0 until end of turn."

    afflict(2)

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Tommy Arnold"
        flavorText = "The God-Pharaoh's benevolence concealed the factory he created to produce an army of Eternals."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c869cc4-eed9-4b24-b60f-2d7f85b885dc.jpg?1783936029"
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
