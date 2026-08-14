package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

private const val ARLINN_EMBLEM = "Creatures you control have haste and '{T}: This creature deals damage equal to its power to any target.'"

private val arlinnEmblemAbility = ActivatedAbility(
    id = AbilityId.generate(),
    cost = Costs.Tap,
    targetRequirements = listOf(AnyTarget()),
    effect = Effects.DealDamage(DynamicAmounts.sourcePower(), EffectTarget.ContextTarget(0)),
    descriptionOverride = "{T}: This creature deals damage equal to its power to any target.",
)

private val ArlinnEmbracedByTheMoon = card("Arlinn, Embraced by the Moon") {
    manaCost = ""
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Arlinn"
    oracleText = "+1: Creatures you control get +1/+1 and gain trample until end of turn.\n" +
        "−1: Arlinn deals 3 damage to any target. Transform Arlinn.\n" +
        "−6: You get an emblem with \"$ARLINN_EMBLEM\""

    loyaltyAbility(+1) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.EndOfTurn),
        )
    }
    loyaltyAbility(-1) {
        val target = target("target", AnyTarget())
        effect = Effects.DealDamage(3, target) then TransformEffect(EffectTarget.Self)
    }
    loyaltyAbility(-6) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter.AllCreaturesYouControl,
            grantedKeywords = listOf(Keyword.HASTE.name),
            grantedActivatedAbilities = listOf(arlinnEmblemAbility),
            emblemDescription = ARLINN_EMBLEM,
        )
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "243"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/back/b/3/b37aa12c-a6b3-4cf8-b5a4-0a999ff12d02.jpg?1783937719"
    }
}

private val ArlinnKordFront = card("Arlinn Kord") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Arlinn"
    startingLoyalty = 3
    oracleText = "+1: Until end of turn, up to one target creature gets +2/+2 and gains vigilance and haste.\n" +
        "0: Create a 2/2 green Wolf creature token. Transform Arlinn Kord."

    loyaltyAbility(+1) {
        val creature = target("creature", TargetCreature(optional = true))
        effect = Effects.ModifyStats(2, 2, creature) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.HASTE, creature, Duration.EndOfTurn)
    }
    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
        ) then TransformEffect(EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "243"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b37aa12c-a6b3-4cf8-b5a4-0a999ff12d02.jpg?1783937719"
    }
}

val ArlinnKord: CardDefinition = ArlinnKordFront.copy(backFace = ArlinnEmbracedByTheMoon)
