package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Scholar of the Ages — Core Set 2020 #74
 * {5}{U}{U} · Creature — Human Wizard · Uncommon
 * 3/3
 *
 * When this creature enters, return up to two target instant and/or sorcery cards from your
 * graveyard to your hand.
 *
 * "Up to two target" is a multi-slot target; [Effects.ForEachTarget] over `ContextTarget(0)`
 * returns each chosen card.
 */
val ScholarOfTheAges = card("Scholar of the Ages") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, return up to two target instant and/or sorcery cards " +
        "from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = TargetObject(filter = TargetFilter.InstantOrSorceryInYourGraveyard, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, return up to two target instant and/or sorcery " +
            "cards from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Micah Epstein"
        flavorText = "\"I study the past to find hope for the future.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80137c9a-ea56-4dc7-a503-43fe192c8fce.jpg?1783933005"
    }
}
