package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Step

/**
 * Dwarven Weaponsmith
 * {1}{R}
 * Creature — Dwarf Artificer
 * 1/1
 * {T}, Sacrifice an artifact: Put a +1/+1 counter on target creature. Activate only during
 * your upkeep.
 *
 * Tap + sacrifice-an-artifact cost; places one +1/+1 counter on the target creature. The
 * ability may only be activated during the controller's upkeep
 * ([ActivationRestriction.OnlyDuringYourTurn] + [ActivationRestriction.DuringStep] UPKEEP).
 */
val DwarvenWeaponsmith = card("Dwarven Weaponsmith") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Artificer"
    power = 1
    toughness = 1
    oracleText = "{T}, Sacrifice an artifact: Put a +1/+1 counter on target creature. " +
        "Activate only during your upkeep."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0848d94a-2704-460f-986b-b192dd6d26b7.jpg?1562896790"
    }
}
