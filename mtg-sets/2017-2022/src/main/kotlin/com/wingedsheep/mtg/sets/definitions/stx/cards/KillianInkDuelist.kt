package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Killian, Ink Duelist — Strixhaven: School of Mages #197 (canonical printing)
 * {W}{B} · Legendary Creature — Human Warlock · 2/2
 *
 * Lifelink
 * Menace
 * Spells you cast that target a creature cost {2} less to cast.
 *
 * The discount is the Strong Back shape: a [SpellCostTarget.YouCast] modifier over every spell,
 * reducing generic mana by [CostReductionSource.FixedIfAnyTargetMatches] when any chosen target is
 * a creature on the battlefield ("a creature" means a creature permanent, so a spell targeting a
 * creature card in a graveyard isn't discounted). Generic-only, so colored requirements stand.
 */
val KillianInkDuelist = card("Killian, Ink Duelist") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Warlock"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Spells you cast that target a creature cost {2} less to cast."

    keywords(Keyword.LIFELINK, Keyword.MENACE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(2, GameObjectFilter.Creature.onBattlefield())
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Ryan Pancoast"
        flavorText = "\"When your father is Dean Embrose, second best really isn't an option.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23ef4f09-2aa1-4a03-b2e2-66d1522f1e46.jpg?1783927314"
        ruling("2021-04-16", "If a spell has additional costs, apply those increases before applying cost reductions.")
        ruling("2021-04-16", "Killian, Ink Duelist can't reduce the colored mana requirement of a spell's mana cost.")
        ruling("2021-04-16", "Killian can reduce alternative costs. For example, if you choose to pay the {1}{B} alternative cost for Baleful Mastery while targeting a creature, that cost would be reduced to {B}.")
    }
}
