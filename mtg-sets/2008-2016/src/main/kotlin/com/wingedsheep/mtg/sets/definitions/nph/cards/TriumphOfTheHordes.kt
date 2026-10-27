package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Triumph of the Hordes
 * {2}{G}{G}
 * Sorcery
 * Until end of turn, creatures you control get +1/+1 and gain trample and infect.
 */
val TriumphOfTheHordes = card("Triumph of the Hordes") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, creatures you control get +1/+1 and gain trample and infect. " +
        "(Creatures with infect deal damage to creatures in the form of -1/-1 counters and to players " +
        "in the form of poison counters.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INFECT, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c16b90ff-d256-4ac6-b687-3430b8c80dd7.jpg?1783941300"
        ruling("2011-06-01", "Multiple instances of infect are redundant.")
    }
}
