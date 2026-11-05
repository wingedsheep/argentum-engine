package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merciless Eternal — Hour of Devastation #71 (canonical printing)
 * {2}{B} · Creature — Zombie Cleric · 2/2
 *
 * Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)
 * {2}{B}, Discard a card: This creature gets +2/+2 until end of turn.
 *
 * Afflict is keyword-only: the engine derives the becomes-blocked trigger (see `Afflict`).
 */
val MercilessEternal = card("Merciless Eternal") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Cleric"
    power = 2
    toughness = 2
    oracleText = "Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)\n{2}{B}, Discard a card: This creature gets +2/+2 until end of turn."

    afflict(2)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.DiscardCard)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Mathias Kollros"
        flavorText = "\"My work here is complete. The people have done everything required of them.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11a6becd-15be-4f2d-8fda-35b1b289b28f.jpg?1783936038"
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
