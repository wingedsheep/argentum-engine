package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Mirrex — Phyrexia: All Will Be One #254
 * Land — Sphere
 *
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Activate only if this land entered this turn.
 * {3}, {T}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and
 * "This token can't block."
 *
 * "Entered this turn" is [Conditions.SourceEnteredThisTurn] — the per-turn entry marker the
 * engine stamps on any permanent entering the battlefield (played or put), cleared at turn end.
 */
val Mirrex = card("Mirrex") {
    typeLine = "Land — Sphere"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Activate only if this land entered this turn.\n" +
        "{3}, {T}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and " +
        "\"This token can't block.\" (Players dealt combat damage by it also get a poison counter.)"

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceEnteredThisTurn)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.CreatePhyrexianMite()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "254"
        artist = "Adam Burn"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54a702cd-ca49-4570-b47e-8b090452a3c3.jpg?1783917980"
    }
}
