package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Potioner's Trove — Secrets of Strixhaven #251
 * {3} · Artifact
 *
 * {T}: Add one mana of any color.
 * {T}: You gain 2 life. Activate only if you've cast an instant or sorcery spell this turn.
 *
 * First ability is a vanilla any-color mana ability (`AddManaOfChoice` — the player picks the
 * color). Second is a lifegain ability gated on the SOS "spells matter" tracker:
 * `Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = InstantOrSorcery)` reads the
 * controller's cast history, so the {T}: gain 2 life ability is only activatable once an instant
 * or sorcery has been cast this turn (countered/fizzled spells still count). Both abilities tap the
 * same permanent, so only one can be used per turn — modeled faithfully as two separate {T} costs.
 */
val PotionersTrove = card("Potioner's Trove") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{T}: You gain 2 life. Activate only if you've cast an instant or sorcery spell this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.GainLife(2)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouCastSpellsThisTurn(
                    atLeast = 1,
                    filter = GameObjectFilter.InstantOrSorcery,
                ),
            ),
        )
        description = "{T}: You gain 2 life. Activate only if you've cast an instant or sorcery spell this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Alessandra Pisano"
        flavorText = "The shops of Ribtruss Town sell anything that can be gathered within Titan's Grave. " +
            "Just don't ask where they acquired their goods."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2123b349-4649-4a15-a8b5-b54414d2b1b7.jpg?1775938752"
    }
}
