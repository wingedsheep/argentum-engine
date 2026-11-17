package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Creatures you control get +2/+2 and gain first strike and lifelink until end of turn. */
private val creaturesGetPumpedFirstStrikeLifelink = Effects.ForEachInGroup(
    GroupFilter(GameObjectFilter.Creature.youControl()),
    Effects.ModifyStats(2, 2, EffectTarget.IterationEntity) then
        Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.IterationEntity) then
        Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.IterationEntity)
)

/**
 * Soul of Theros
 * {4}{W}{W}
 * Creature — Avatar
 * 6/6
 * Vigilance
 * {4}{W}{W}: Creatures you control get +2/+2 and gain first strike and lifelink until end of turn.
 * {4}{W}{W}, Exile this card from your graveyard: Creatures you control get +2/+2 and gain first strike and lifelink until end of turn.
 *
 * Both abilities share one effect; the second is activated from the graveyard with
 * exiling this card as part of its cost.
 */
val SoulOfTheros = card("Soul of Theros") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 6
    oracleText = "Vigilance\n" +
        "{4}{W}{W}: Creatures you control get +2/+2 and gain first strike and lifelink until end of turn.\n" +
        "{4}{W}{W}, Exile this card from your graveyard: Creatures you control get +2/+2 and gain first strike and lifelink until end of turn."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{4}{W}{W}")
        effect = creaturesGetPumpedFirstStrikeLifelink
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}{W}"), Costs.ExileSelf)
        effect = creaturesGetPumpedFirstStrikeLifelink
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63e391d3-cf19-4f73-9d39-22587e0f3c0d.jpg"
    }
}
