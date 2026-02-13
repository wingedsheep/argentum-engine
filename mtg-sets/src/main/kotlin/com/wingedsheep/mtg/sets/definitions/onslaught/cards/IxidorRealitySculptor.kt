package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.TurnFaceUpEffect
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Ixidor, Reality Sculptor
 * {3}{U}{U}
 * Legendary Creature — Human Wizard
 * 3/4
 * Face-down creatures get +1/+1.
 * {2}{U}: Turn target face-down creature face up.
 */
val IxidorRealitySculptor = card("Ixidor, Reality Sculptor") {
    manaCost = "{3}{U}{U}"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "Face-down creatures get +1/+1.\n{2}{U}: Turn target face-down creature face up."

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.faceDown())
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.faceDown())
        )
        effect = TurnFaceUpEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Kev Walker"
        flavorText = "Reality has exiled me. I am no longer bound by its laws."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/314d5e89-55f7-42b4-af19-d4d0f499a265.jpg?1562906514"
    }
}
