package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lyse Hext
 * {1}{W}{U}
 * Legendary Creature — Human Rebel Monk
 * 2/2
 * Prowess
 * Noncreature spells you cast cost {1} less to cast.
 * As long as you've cast two or more noncreature spells this turn, Lyse Hext has double strike.
 *
 * - Prowess is `prowess()` (keyword plus its trigger), not a bare `Keyword.PROWESS`.
 * - The discount is Blood Funnel's `YouCast(Noncreature)` reduction (generic mana only, ruling).
 * - The double strike is a self-granting [ConditionalStaticAbility] over
 *   `spellsCastThisTurn(You, Noncreature) >= 2`, which reads the turn's cast history — so spells
 *   cast before Lyse entered count (ruling), and a countered spell still counts as cast.
 */
val LyseHext = card("Lyse Hext") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Rebel Monk"
    power = 2
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Noncreature spells you cast cost {1} less to cast.\n" +
        "As long as you've cast two or more noncreature spells this turn, Lyse Hext has double strike."

    prowess()

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Noncreature),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.Self),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.spellsCastThisTurn(Player.You, GameObjectFilter.Noncreature),
                ComparisonOperator.GTE,
                2,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Ramza Psyru"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97845ece-ceef-468c-a4e3-6fde1413c9f0.jpg?1783906345"
        ruling("2025-06-06", "Noncreature spells that were cast before Lyse Hext entered count. For example, if Lyse Hext enters on a turn where you've already cast two noncreature spells, she'll have double strike immediately.")
        ruling("2025-06-06", "The cost reduction applies only to generic mana in the total cost of noncreature spells you cast.")
    }
}
