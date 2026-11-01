package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tithe Taker — Ravnica Allegiance #27
 * {1}{W} · Creature — Human Soldier · 2/1
 *
 * The one printed sentence is two cost statics sharing a "during your turn" gate: the spell half
 * is Paladin Class's opponents'-spells tax gated on [Conditions.IsYourTurn]; the ability half is an
 * [IncreaseActivatedAbilityCost] with the [ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS] reach —
 * the activator decides, so an opponent's cycling from hand pays too — wrapped in a
 * [ConditionalStaticAbility] on the same condition. A no-mana ability ("{T}:") becomes "{1}, {T}:"
 * (printed ruling). Afterlife is composed as its reminder text, as on Orzhov Enforcer.
 */
val TitheTaker = card("Tithe Taker") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "During your turn, spells your opponents cast cost {1} more to cast and abilities your " +
        "opponents activate cost {1} more to activate unless they're mana abilities.\n" +
        "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
            modification = CostModification.IncreaseGeneric(1),
            gating = CostGating.OnlyIf(Conditions.IsYourTurn),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = IncreaseActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Any),
                amount = DynamicAmounts.fixed(1),
                excludeManaAbilities = true,
                reach = ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS,
            ),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd26b7b1-992d-4b8c-bc33-51aab5abdf98.jpg?1783933715"
        ruling(
            "2019-01-25",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases (such as that of Tithe Taker's effect), then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was."
        )
        ruling(
            "2019-01-25",
            "An opponent's activated ability that costs no mana to activate will cost {1} plus its non-mana costs during your turn."
        )
        ruling(
            "2019-01-25",
            "Because blockers are chosen all at once, you can't block with a creature with afterlife, wait for it to die, then block with the resulting Spirit tokens."
        )
    }
}
