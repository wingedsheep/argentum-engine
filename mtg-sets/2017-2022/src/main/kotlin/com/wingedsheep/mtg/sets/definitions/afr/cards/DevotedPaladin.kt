package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devoted Paladin
 * {4}{W}
 * Creature — Orc Knight
 * 4/4
 * Beacon of Hope — When this creature enters, creatures you control get +1/+1 and gain vigilance
 * until end of turn.
 */
val DevotedPaladin = card("Devoted Paladin") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Orc Knight"
    oracleText = "Beacon of Hope — When this creature enters, creatures you control get +1/+1 and " +
        "gain vigilance until end of turn."
    power = 4
    toughness = 4
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
        collectorNumber = "11"
        artist = "Chris Rallis"
        flavorText = "\"I swore a sacred oath to protect the weak and punish those who threaten them. " +
            "As long as I draw breath, I will protect you.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3620ed96-1d15-4942-b9e5-9f9a64b0cab4.jpg?1783926534"
    }
}
