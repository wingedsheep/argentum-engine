package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Priest of the Haunted Edge — Kaldheim #104
 * {1}{B} · Snow Creature — Zombie Cleric · 0/4
 *
 * {T}, Sacrifice this creature: Target creature gets -X/-X until end of turn, where X is the number
 * of snow lands you control. Activate only as a sorcery.
 *
 * Lys Alana Scarblade's shrink with a snow-land tally: X is an `AggregateBattlefield` count read as
 * the ability resolves (2021-02-05 ruling), negated with unary minus.
 */
val PriestOfTheHauntedEdge = card("Priest of the Haunted Edge") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Snow Creature — Zombie Cleric"
    power = 0
    toughness = 4
    oracleText = "{T}, Sacrifice this creature: Target creature gets -X/-X until end of turn, where X is " +
        "the number of snow lands you control. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val victim = target(TargetFilter.Creature)
        val shrink = -DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.snow()).count()
        effect = Effects.ModifyStats(shrink, shrink, victim)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cde0f4d-5acc-4a25-a3d6-c6b9b734360c.jpg?1783928244"
        ruling("2021-02-05", "Use the number of snow lands you control as the ability resolves to determine the value of X.")
    }
}
