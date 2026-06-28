package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Blitzball
 * {3}
 * Artifact
 *
 * {T}: Add one mana of any color.
 * GOOOOAAAALLL! — {T}, Sacrifice this artifact: Draw two cards. Activate only if an
 * opponent was dealt combat damage by a legendary creature this turn.
 *
 * The mana ability is the standard `AddAnyColorMana` rock. The draw ability gates on a new
 * per-turn tracker — [Conditions.AnOpponentWasDealtCombatDamageByLegendaryCreatureThisTurn],
 * backed by `DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE` — recorded in `CombatDamageManager`
 * whenever a legendary creature deals combat damage to a player and cleared at end of turn.
 */
val Blitzball = card("Blitzball") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "GOOOOAAAALLL! — {T}, Sacrifice this artifact: Draw two cards. Activate only if an " +
        "opponent was dealt combat damage by a legendary creature this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(2)
        description = "GOOOOAAAALLL! — Draw two cards. Activate only if an opponent was dealt " +
            "combat damage by a legendary creature this turn."
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.AnOpponentWasDealtCombatDamageByLegendaryCreatureThisTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Gas1"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92f4ad73-42bf-45c0-8bb6-0b44043c81ef.jpg?1748706744"
    }
}
