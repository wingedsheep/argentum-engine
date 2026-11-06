package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dawnfeather Eagle
 * {4}{W}
 * Creature — Bird
 * 3/3
 * Flying
 * When this creature enters, creatures you control get +1/+1 and gain vigilance until end of turn.
 */
val DawnfeatherEagle = card("Dawnfeather Eagle") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, creatures you control get +1/+1 and gain vigilance until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"It came to rest upon the angel's arm, and then the weight of their gazes fell upon me.\"\n—Aeronaut's journal"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7113ddc-cb3c-46da-a6c7-2567bd0affa9.jpg?1783936780"
        ruling("2017-02-09", "The set of creatures affected by Dawnfeather Eagle's triggered ability is determined as the ability resolves. Creatures you begin to control later in the turn and permanents you control that become creatures later in the turn won't get +1/+1 or gain vigilance.")
        ruling("2017-02-09", "Dawnfeather Eagle's triggered ability affects itself.")
    }
}
