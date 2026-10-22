package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brimstone Vandal
 * {2}{R}
 * Creature — Devil
 * 2/3
 * Menace
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, this creature deals 1 damage to each opponent.
 */
val BrimstoneVandal = card("Brimstone Vandal") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Devil"
    power = 2
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, this creature deals 1 damage to each opponent."

    keywords(Keyword.MENACE)

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10addf9a-daca-4b84-a9d3-f98f7b955af9.jpg?1783925604"
    }
}
