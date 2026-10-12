package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hajar, Loyal Bodyguard
 * {R}{G}
 * Legendary Creature — Human Soldier
 * 3/3
 * Sacrifice Hajar: Legendary creatures you control get +1/+0 and gain indestructible until end of turn.
 *
 * The affected set is snapshotted as the ability resolves; Hajar is already gone (sacrificed as a
 * cost), so it never benefits.
 */
val HajarLoyalBodyguard = card("Hajar, Loyal Bodyguard") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Sacrifice Hajar: Legendary creatures you control get +1/+0 and gain indestructible until end of turn."

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.legendary().youControl()),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Joshua Raphael"
        flavorText = "\"I've stood by Mishra's side from the beginning. I will not abandon him now.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a0a2f82-57d3-45d8-b1c8-357883c62728.jpg?1783920031"
    }
}
