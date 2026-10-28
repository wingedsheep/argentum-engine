package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Circle of Flame — Magic 2012 #127
 * {1}{R} · Enchantment
 *
 * Whenever a creature without flying attacks you or a planeswalker you control, this enchantment
 * deals 1 damage to that creature.
 *
 * An ANY-binding attack trigger fires once per declared attacker matching the filter, read
 * against projected state at declaration — so a creature put onto the battlefield attacking, or
 * one that loses flying after attacking, never triggers it (2014-07-18 ruling).
 */
val CircleOfFlame = card("Circle of Flame") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature without flying attacks you or a planeswalker you control, this " +
        "enchantment deals 1 damage to that creature."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING).attackingYouOrYourPlaneswalkers()
        ).attacks()
        effect = Effects.DealDamage(1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Jaime Jones"
        flavorText = "\"Which do you think is a better deterrent: a moat of water or one of fire?\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/419b1813-9760-47b9-b6f3-e501586cfe4d.jpg?1783941072"
        ruling(
            "2014-07-18",
            "Circle of Flame's ability triggers only if a creature without flying is declared as an attacker " +
                "during the declare attackers step. If a creature with flying attacks you or a planeswalker you " +
                "control and then loses flying, or if a creature without flying is put onto the battlefield " +
                "attacking you or a planeswalker you control, the ability won't trigger."
        )
    }
}
