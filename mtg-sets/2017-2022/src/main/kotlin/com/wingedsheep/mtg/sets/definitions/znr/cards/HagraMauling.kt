package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hagra Mauling {2}{B}{B} // Hagra Broodpit — Zendikar Rising #106
 * Instant // Land · Modal double-faced card
 *
 * Front — Hagra Mauling: "This spell costs {1} less to cast if an opponent controls no basic
 * lands. Destroy target creature."
 * Back — Hagra Broodpit: "This land enters tapped. {T}: Add {B}."
 *
 * "An opponent controls no basic lands" is existential over opponents (per the ruling, one
 * basic-less opponent is enough even if others control basics), so it is the *fewest* basic
 * lands any single opponent controls compared against zero — [DynamicAmounts.leastAmongPlayers]
 * rebinds `Player.You` inside the count to each opponent in turn. A pooled
 * `Exists(EachOpponent, negate = true)` would wrongly require *every* opponent to lack basics.
 * The reduction is a flat {1} regardless of how many opponents qualify.
 */
private val HagraMaulingFront = card("Hagra Mauling") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if an opponent controls no basic lands.\n" +
        "Destroy target creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(
                Conditions.CompareAmounts(
                    DynamicAmounts.leastAmongPlayers(
                        DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.BasicLand),
                        Player.EachOpponent,
                    ),
                    ComparisonOperator.EQ,
                    0,
                )
            )
        )
    }

    spell {
        val victim = target(TargetFilter.Creature)
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Victor Adame Minguez"
        flavorText = "\"Din, turn around! Now!\"\n—Doros, expedition guide"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c04c734-354d-4925-8161-7052110951df.jpg?1783929380"
        ruling("2020-09-25", "Hagra Mauling's first ability reduces the cost by only {1}, even if you have two opponents with no basic lands. It reduces the cost even if some opponents control basic lands, as long as one controls no basic lands.")
        ruling("2020-09-25", "The opponent who controls no basic lands doesn't have to be the controller of the target creature for Hagra Mauling's cost to be reduced.")
    }
}

private val HagraBroodpitBack = card("Hagra Broodpit") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "This land enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Victor Adame Minguez"
        flavorText = "\"I think we somehow got turned around.\"\n—Doros, expedition guide"
        imageUri = "https://cards.scryfall.io/normal/back/7/c/7c04c734-354d-4925-8161-7052110951df.jpg?1783929380"
    }
}

val HagraMauling: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = HagraMaulingFront,
    backFace = HagraBroodpitBack,
)
