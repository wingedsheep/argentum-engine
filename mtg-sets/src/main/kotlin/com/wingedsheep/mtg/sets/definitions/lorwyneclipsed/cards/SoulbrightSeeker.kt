package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Soulbright Seeker
 * {R}
 * Creature — Elemental Sorcerer
 * 2/1
 *
 * As an additional cost to cast this spell, behold an Elemental or pay {2}.
 * (To behold an Elemental, choose an Elemental you control or reveal an Elemental card from your hand.)
 * {R}: Target creature you control gains trample until end of turn. If this is the third time this
 * ability has resolved this turn, add {R}{R}{R}{R}.
 */
val SoulbrightSeeker = card("Soulbright Seeker") {
    manaCost = "{R}"
    typeLine = "Creature — Elemental Sorcerer"
    power = 2
    toughness = 1
    oracleText = "As an additional cost to cast this spell, behold an Elemental or pay {2}. " +
        "(To behold an Elemental, choose an Elemental you control or reveal an Elemental card from your hand.)\n" +
        "{R}: Target creature you control gains trample until end of turn. If this is the third time " +
        "this ability has resolved this turn, add {R}{R}{R}{R}."

    additionalCost(
        AdditionalCost.BeholdOrPay(
            filter = Filters.WithSubtype("Elemental"),
            alternativeManaCost = "{2}"
        )
    )

    activatedAbility {
        cost = Costs.Mana("{R}")
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            .then(IncrementAbilityResolutionCountEffect)
            .then(
                ConditionalEffect(
                    condition = Conditions.SourceAbilityResolvedNTimes(3),
                    effect = Effects.AddMana(Color.RED, amount = 4)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Kev Fang"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/895ac890-608a-47de-8bc8-9337fd2064e8.jpg?1767957180"
    }
}
