package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rowan, Fearless Sparkmage — Throne of Eldraine #304 (canonical printing)
 * {3}{R}{R} · Legendary Planeswalker — Rowan · Loyalty 5
 *
 * +1: Up to one target creature gets +3/+0 and gains first strike until end of turn.
 * −2: Rowan deals 1 damage to each of up to two target creatures. Those creatures can't block this turn.
 * −9: Gain control of all creatures until end of turn. Untap them. They gain haste until end of turn.
 *
 * The −2 is one "up to two" requirement walked by [Effects.ForEachTarget]; the can't-block applies to
 * each chosen target regardless of whether the damage was prevented (ruling). The −9 is the
 * Insurrection shape: control, untap and haste over every creature.
 */
val RowanFearlessSparkmage = card("Rowan, Fearless Sparkmage") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Rowan"
    startingLoyalty = 5
    oracleText = "+1: Up to one target creature gets +3/+0 and gains first strike until end of turn.\n" +
        "−2: Rowan deals 1 damage to each of up to two target creatures. Those creatures can't block this turn.\n" +
        "−9: Gain control of all creatures until end of turn. Untap them. They gain haste until end of turn."

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(3, 0, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    loyaltyAbility(-2) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(1, EffectTarget.ContextTarget(0)) then
                Effects.CantBlock(EffectTarget.ContextTarget(0))
        )
    }

    loyaltyAbility(-9) {
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn)) then
            Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.Untap(EffectTarget.IterationEntity)) then
            Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "304"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/030f4058-54e5-4333-bd6c-2789c334bf12.jpg?1783932556"
        ruling("2019-10-04", "Rowan's second ability can't target the same creature twice to deal 2 damage to it.")
        ruling("2019-10-04", "If a target of Rowan's second ability is a legal target but an effect prevents the damage that would be dealt to it, that creature still can't block this turn.")
        ruling("2019-10-04", "Rowan's third ability can affect creatures you already control or ones that are already untapped. It will untap them if applicable and give them haste.")
        ruling("2019-10-04", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
    }
}
