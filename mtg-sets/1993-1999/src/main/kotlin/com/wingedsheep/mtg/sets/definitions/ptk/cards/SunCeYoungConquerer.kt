package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sun Ce, Young Conquerer
 * {3}{U}{U}
 * Legendary Creature — Human Soldier
 * 3/3
 * Horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 * When Sun Ce enters, you may return target creature to its owner's hand.
 *
 * The target is mandatory at announcement — the trigger carries a `targetRequirement`, so a
 * creature is chosen when the ability goes on the stack — and the printed "you may" is only the
 * resolution-time yes/no (`optional = true` lowers to a `Gate.MayDecide`).
 */
val SunCeYoungConquerer = card("Sun Ce, Young Conquerer") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "When Sun Ce enters, you may return target creature to its owner's hand."

    keywords(Keyword.HORSEMANSHIP)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val victim = target("target", TargetObject(filter = TargetFilter.Creature))
        effect = Effects.ReturnToHand(victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Yang Guangmai"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16114a68-58d1-4aad-9b3a-890e9c84b253.jpg"
    }
}
