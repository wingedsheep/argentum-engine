package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Fire Bowman
 * {R}
 * Creature — Human Soldier Archer
 * 1/1
 * Sacrifice this creature: It deals 1 damage to any target. Activate only during your turn,
 * before attackers are declared.
 *
 * The Portal timing rider is two [ActivationRestriction]s, not one: [ActivationRestriction.OnlyDuringYourTurn]
 * for "during your turn" and [ActivationRestriction.BeforeStep] for "before attackers are declared".
 */
val FireBowman = card("Fire Bowman") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier Archer"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: It deals 1 damage to any target. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = AbilityCost.SacrificeSelf
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val victim = target("target", Targets.Any)
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Cai Tingting"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18acc721-e9ae-4ba4-b435-754e41fb541e.jpg"
    }
}
