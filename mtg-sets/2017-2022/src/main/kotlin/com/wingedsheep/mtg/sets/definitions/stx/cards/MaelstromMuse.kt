package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Maelstrom Muse — Strixhaven: School of Mages #202 (canonical printing)
 * {1}{U}{U/R}{R} · Creature — Djinn Wizard · 2/4
 *
 * Flying
 * Whenever this creature attacks, the next instant or sorcery spell you cast this turn costs {X}
 * less to cast, where X is this creature's power as this ability resolves.
 *
 * [Effects.ReduceNextSpellCost] is the one-shot `ModifySpellCostsEffect(nextSpellOnly = true)`
 * rider: its amount is evaluated once, as the trigger resolves (the source's projected power, or
 * its last-known power if it has left the battlefield), and the entry is consumed by the next
 * matching instant or sorcery cast this turn. Only generic mana is reduced.
 */
val MaelstromMuse = card("Maelstrom Muse") {
    manaCost = "{1}{U}{U/R}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Djinn Wizard"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever this creature attacks, the next instant or sorcery spell you cast this turn costs " +
        "{X} less to cast, where X is this creature's power as this ability resolves."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReduceNextSpellCost(
            DynamicAmounts.sourcePower(),
            GameObjectFilter.InstantOrSorcery
        )
        description = "Whenever this creature attacks, the next instant or sorcery spell you cast " +
            "this turn costs {X} less to cast, where X is this creature's power as this ability resolves."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Michele Parisi"
        flavorText = "She's an inspiration to everyone, even herself."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5516e81e-16ea-411c-9df0-ed3b03670220.jpg?1783927307"
        ruling("2021-04-16", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Maelstrom Muse's triggered ability). The mana value of the spell is determined only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2021-04-16", "Maelstrom Muse's cost reduction can reduce only generic mana in a spell's total cost. It can't reduce colored mana requirements.")
    }
}
