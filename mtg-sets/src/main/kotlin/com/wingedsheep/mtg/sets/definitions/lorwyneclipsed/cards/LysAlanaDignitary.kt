package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Lys Alana Dignitary
 * {1}{G}
 * Creature — Elf Advisor
 * 2/3
 *
 * As an additional cost to cast this spell, behold an Elf or pay {2}.
 * (To behold an Elf, choose an Elf you control or reveal an Elf card from your hand.)
 * {T}: Add {G}{G}. Activate only if there is an Elf card in your graveyard.
 */
val LysAlanaDignitary = card("Lys Alana Dignitary") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Advisor"
    power = 2
    toughness = 3
    oracleText = "As an additional cost to cast this spell, behold an Elf or pay {2}. " +
        "(To behold an Elf, choose an Elf you control or reveal an Elf card from your hand.)\n" +
        "{T}: Add {G}{G}. Activate only if there is an Elf card in your graveyard."

    additionalCost(
        AdditionalCost.BeholdOrPay(
            filter = Filters.WithSubtype("Elf"),
            alternativeManaCost = "{2}"
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.GREEN, amount = 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.GraveyardContainsSubtype(Subtype.ELF))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94e8d6a9-7aa3-4e93-8e4d-e50da7ff09d2.jpg?1767957201"
    }
}
