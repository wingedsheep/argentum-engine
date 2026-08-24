package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Flanking Troops
 * {2}{W}{W}
 * Creature — Human Soldier
 */
val FlankingTroops = card("Flanking Troops") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, you may tap target creature."

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        val creature = target("target", Targets.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Li Wang"
        flavorText = "Following the battle of Redcliffs, both Liu Bei and Sun Quan coveted the province of Jingzhou. After Sun Quan's troops failed to capture it, Liu Bei's succeeded."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a219a031-2466-4850-b646-79a09e30cf18.jpg"
    }
}
