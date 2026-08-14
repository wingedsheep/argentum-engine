package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rowan, Scion of War
 * {1}{B}{R}
 * Legendary Creature — Human Wizard
 * 4/2
 *
 * Menace
 * {T}: Spells you cast this turn that are black and/or red cost {X} less to cast, where X is
 * the amount of life you lost this turn. Activate only as a sorcery.
 *
 * The black/red twin of [WillScionOfPeace] — same turn-scoped, state-held discount
 * ([Effects.ReduceSpellCostsThisTurn]), reading the life-lost accumulator instead of life gained.
 * Damage taken, life-loss effects and life paid as a cost all feed that total, and life gained
 * never nets against it.
 */
val RowanScionOfWar = card("Rowan, Scion of War") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 4
    toughness = 2
    oracleText = "Menace\n" +
        "{T}: Spells you cast this turn that are black and/or red cost {X} less to cast, " +
        "where X is the amount of life you lost this turn. Activate only as a sorcery."

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        effect = Effects.ReduceSpellCostsThisTurn(
            spellFilter = GameObjectFilter.Any.withAnyColor(Color.BLACK, Color.RED),
            amount = DynamicAmounts.lifeLostThisTurn(),
        )
        description = "{T}: Spells you cast this turn that are black and/or red cost {X} less to cast, " +
            "where X is the amount of life you lost this turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Magali Villeneuve"
        flavorText = "\"No more days withering at a negotiating table with my brother. " +
            "I will rebuild Eldraine myself—the old way, with power.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ee179ab-a15b-4bd6-b7f8-1e1abeeb31b7.jpg?1783915070"

        ruling(
            "2023-09-01",
            "The value of X is determined only once, at the time Rowan, Scion of War's activated " +
                "ability resolves."
        )
        ruling(
            "2023-09-01",
            "Rowan's activated ability doesn't change the mana cost or mana value of any spell. It " +
                "changes only the total cost you pay."
        )
        ruling(
            "2023-09-01",
            "Rowan's activated ability can't reduce the amount of colored mana you pay for a spell. " +
                "It reduces only the generic mana component of that cost."
        )
        ruling(
            "2023-09-01",
            "Rowan's activated ability counts the total amount of life you lost without taking into " +
                "account any life you gained during that turn. For example, if you gained 3 life and " +
                "lost 3 life earlier in the turn, the cost of black and/or red spells you cast this " +
                "turn will be reduced by {3}."
        )
    }
}
