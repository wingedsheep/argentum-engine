package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chandra, Novice Pyromancer
 * {3}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty: 5
 *
 * +1: Elementals you control get +2/+0 until end of turn.
 * −1: Add {R}{R}.
 * −2: Chandra deals 2 damage to any target.
 *
 * The +1 locks in the Elementals you control as it resolves (per-entity pump, not a continuous
 * group effect). The −1 is a loyalty ability, so it is not a mana ability and uses the stack.
 */
val ChandraNovicePyromancer = card("Chandra, Novice Pyromancer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 5
    oracleText = "+1: Elementals you control get +2/+0 until end of turn.\n" +
        "−1: Add {R}{R}.\n" +
        "−2: Chandra deals 2 damage to any target."

    loyaltyAbility(+1) {
        effect = Patterns.Group.modifyStatsForAll(
            2, 0,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Elemental").youControl())
        )
        description = "Elementals you control get +2/+0 until end of turn."
    }

    loyaltyAbility(-1) {
        effect = Effects.AddMana(Color.RED, 2)
        description = "Add {R}{R}."
    }

    loyaltyAbility(-2) {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(2, any)
        description = "Chandra deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d678cf4c-60e3-40a1-a9cc-b0bd157bcf36.jpg?1783932982"
        ruling("2019-07-12", "Chandra's first ability affects only Elementals you control at the time it resolves. Elementals you begin to control later in the turn won't get +2/+0.")
        ruling("2019-07-12", "Because it's a loyalty ability, Chandra's second ability isn't a mana ability. It can be activated only any time you could cast a sorcery. It uses the stack and can be responded to.")
    }
}
