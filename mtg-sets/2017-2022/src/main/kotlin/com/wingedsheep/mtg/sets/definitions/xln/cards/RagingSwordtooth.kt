package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val RagingSwordtooth = card("Raging Swordtooth") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 5
    oracleText = "Trample\nWhen this creature enters, it deals 1 damage to each other creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature, excludeSelf = true),
            Effects.DealDamage(1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Izzy"
        flavorText = "Carnivorous dinosaurs developed a taste for undead flesh, tracking the vampires by the scent of blood on their breath."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15a79137-9589-4c24-9bea-31041fd3c9ae.jpg?1783935711"
    }
}
