package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shipwreck Singer
 * {U}{B}
 * Creature — Siren
 * 1/2
 * Flying
 * {1}{U}: Target creature an opponent controls attacks this turn if able.
 * {1}{B}, {T}: Attacking creatures get -1/-1 until end of turn.
 *
 * The -1/-1 hits every attacking creature, snapshotted at resolution.
 */
val ShipwreckSinger = card("Shipwreck Singer") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Siren"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "{1}{U}: Target creature an opponent controls attacks this turn if able.\n" +
        "{1}{B}, {T}: Attacking creatures get -1/-1 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.MarkMustAttackThisTurn(creature)
        description = "Target creature an opponent controls attacks this turn if able."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity),
        )
        description = "Attacking creatures get -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Daarken"
        flavorText = "Her melody melds death and beauty with such artistry that even the gods weep to hear it."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13ad0c03-66d9-4fe3-bc0d-ca67d959c285.jpg?1783939725"
    }
}
