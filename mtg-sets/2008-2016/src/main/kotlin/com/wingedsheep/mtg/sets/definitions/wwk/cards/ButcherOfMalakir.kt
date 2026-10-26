package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Butcher of Malakir
 * {5}{B}{B}
 * Creature — Vampire Warrior
 * 5/4
 *
 * Flying
 * Whenever this creature or another creature you control dies, each opponent sacrifices a
 * creature of their choice.
 *
 * Canonical printing: Worldwake, the card's earliest real printing. The dies trigger uses the
 * "a creature you control" binding so the Butcher's own death fires it too.
 */
val ButcherOfMalakir = card("Butcher of Malakir") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warrior"
    power = 5
    toughness = 4
    oracleText = "Flying\nWhenever this creature or another creature you control dies, each opponent sacrifices a " +
        "creature of their choice."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Jason Chan"
        flavorText = "His verdict is always guilty. His sentence is always death."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73131341-0fde-4eca-aefa-ce69c933af07.jpg?1783942056"
        ruling(
            "2010-03-01",
            "If multiple creatures you control (possibly including Butcher of Malakir itself) are put into their " +
                "owners' graveyards at the same time, Butcher of Malakir's triggered ability will trigger that many times."
        )
    }
}
