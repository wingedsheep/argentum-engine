package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boros Elite
 * {W}
 * Creature — Human Soldier
 * 1/1
 * Battalion — Whenever this creature and at least two other creatures attack, this creature gets
 * +2/+2 until end of turn.
 */
val BorosElite = card("Boros Elite") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Willian Murai"
        flavorText = "\"First, you must fight among the rank and file. It is there I will discern your skills as a leader.\"\n—Aurelia, to Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a03974e6-aced-4664-8c5c-3190bb1eb233.jpg?1783940144"
    }
}
