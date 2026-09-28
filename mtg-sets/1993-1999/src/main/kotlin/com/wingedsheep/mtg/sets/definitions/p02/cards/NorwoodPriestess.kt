package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Norwood Priestess
 * {2}{G}{G}
 * Creature — Elf Druid
 * 1/1
 * {T}: You may put a green creature card from your hand onto the battlefield. Activate only
 * during your turn, before attackers are declared.
 */
val NorwoodPriestess = card("Norwood Priestess") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "{T}: You may put a green creature card from your hand onto the battlefield. " +
        "Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.withColor(Color.GREEN),
            count = 1,
            prompt = "Put a green creature onto the battlefield?",
        )
        description = "{T}: You may put a green creature card from your hand onto the battlefield. " +
            "Activate only during your turn, before attackers are declared."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7afd472f-1ede-4e78-b44c-3298ee4c8694.jpg?1783946451"
    }
}
