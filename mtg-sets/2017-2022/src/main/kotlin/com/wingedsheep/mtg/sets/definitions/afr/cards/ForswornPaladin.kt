package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.ManaPayment

/**
 * Forsworn Paladin
 * {B}
 * Creature — Human Knight
 * 1/1
 * Menace
 * {1}{B}, {T}, Pay 1 life: Create a Treasure token.
 * {2}{B}: Target creature gets +2/+0 until end of turn. If mana from a Treasure was spent to
 * activate this ability, that creature also gains deathtouch until end of turn.
 *
 * The Treasure check reads the ability's own saved payment
 * (`manaSpentFromSubtype(TREASURE, ACTIVATION)`), the same provenance Jetmir's Fixer uses.
 */
val ForswornPaladin = card("Forsworn Paladin") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    oracleText = "Menace\n{1}{B}, {T}, Pay 1 life: Create a Treasure token.\n{2}{B}: Target creature gets +2/+0 " +
        "until end of turn. If mana from a Treasure was spent to activate this ability, that creature also gains " +
        "deathtouch until end of turn."
    power = 1
    toughness = 1
    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap, Costs.PayLife(1))
        effect = Effects.CreateTreasure()
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE, ManaPayment.ACTIVATION),
                ComparisonOperator.GTE,
                1,
            ),
            then = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Lorenzo Mastroianni"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4acaf396-f950-42da-85ab-149ffb31fee6.jpg?1783926494"
    }
}
