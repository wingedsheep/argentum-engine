package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Acidic Slime
 * {3}{G}{G}
 * Creature — Ooze
 * 2/2
 * Deathtouch
 * When this creature enters, destroy target artifact, enchantment, or land.
 */
val AcidicSlime = card("Acidic Slime") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    power = 2
    toughness = 2
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "When this creature enters, destroy target artifact, enchantment, or land."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.ArtifactEnchantmentOrLand)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1377f45-edee-4922-825b-6f22163ff63d.jpg?1783942367"
    }
}
