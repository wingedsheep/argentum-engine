package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kaza, Roil Chaser — Zendikar Rising #225 (canonical printing)
 * {U}{R} · Legendary Creature — Human Wizard · 1/2
 *
 * Flying, haste
 * {T}: The next instant or sorcery spell you cast this turn costs {X} less to cast, where X is
 * the number of Wizards you control as this ability resolves.
 *
 * [Effects.ReduceNextSpellCost] evaluates its amount once, as the ability resolves, and the rider
 * is consumed by the next instant or sorcery you cast this turn (generic mana only) — the same
 * shape as Maelstrom Muse, with X counting Wizard permanents you control.
 */
val KazaRoilChaser = card("Kaza, Roil Chaser") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Flying, haste\n" +
        "{T}: The next instant or sorcery spell you cast this turn costs {X} less to cast, " +
        "where X is the number of Wizards you control as this ability resolves."

    keywords(Keyword.FLYING, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ReduceNextSpellCost(
            DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Any.withSubtype("Wizard")),
            GameObjectFilter.InstantOrSorcery
        )
        description = "The next instant or sorcery spell you cast this turn costs {X} less to cast, " +
            "where X is the number of Wizards you control as this ability resolves."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Ekaterina Burmak"
        flavorText = "\"What sort of life is that, huddling in the familiar and safe? No thanks. " +
            "Put me right in the center of the action.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af6bc333-79c0-4de8-9874-80f2254f9ad2.jpg?1783929323"
        ruling("2020-09-25", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Kaza's activated ability). The mana value of the spell is determined only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2020-09-25", "Kaza's cost reduction can reduce only generic mana in a spell's cost. It can't reduce colored mana requirements.")
    }
}
