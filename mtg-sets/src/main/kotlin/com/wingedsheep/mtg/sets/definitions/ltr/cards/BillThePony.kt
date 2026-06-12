package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bill the Pony
 * {3}{W}
 * Legendary Creature — Horse
 * 1/4
 *
 * When Bill the Pony enters, create two Food tokens. (They're artifacts with
 * "{2}, {T}, Sacrifice this token: You gain 3 life.")
 * Sacrifice a Food: Until end of turn, target creature you control assigns combat damage equal to
 * its toughness rather than its power.
 *
 * The combat-damage-as-toughness clause is granted as the [AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS]
 * floating flag (unconditional, until end of turn); CombatDamageUtils consults it via projected
 * keywords. This mirrors the static [com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness]
 * used by Bark of Doran / Doran, but as a turn-scoped grant.
 */
val BillThePony = card("Bill the Pony") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Horse"
    power = 1
    toughness = 4
    oracleText = "When Bill the Pony enters, create two Food tokens. (They're artifacts with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Sacrifice a Food: Until end of turn, target creature you control assigns combat damage " +
        "equal to its toughness rather than its power."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood(2)
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.GrantKeyword(
            AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS,
            creature,
            Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ac68519-ed7f-4f38-9549-c02975f88eed.jpg?1686967660"
    }
}
