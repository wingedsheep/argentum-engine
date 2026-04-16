package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wistfulness
 * {3}{G/U}{G/U}
 * Creature — Elemental Incarnation
 * 6/5
 *
 * When this creature enters, if {G}{G} was spent to cast it, exile target artifact or enchantment
 * an opponent controls.
 * When this creature enters, if {U}{U} was spent to cast it, draw two cards, then discard a card.
 * Evoke {G/U}{G/U}
 */
val Wistfulness = card("Wistfulness") {
    manaCost = "{3}{G/U}{G/U}"
    typeLine = "Creature — Elemental Incarnation"
    power = 6
    toughness = 5
    oracleText = "When this creature enters, if {G}{G} was spent to cast it, exile target artifact or enchantment an opponent controls.\nWhen this creature enters, if {U}{U} was spent to cast it, draw two cards, then discard a card.\nEvoke {G/U}{G/U}"

    evoke = "{G/U}{G/U}"

    // Green gate defined first (goes on stack first, resolves second)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredGreen = 2)
        val artifactOrEnchantment = target(
            "artifact or enchantment an opponent controls",
            TargetObject(filter = TargetFilter.ArtifactOrEnchantment.opponentControls())
        )
        effect = Effects.Exile(artifactOrEnchantment)
    }

    // Blue gate defined second (goes on stack second, resolves first)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredBlue = 2)
        effect = EffectPatterns.loot(draw = 2, discard = 1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "252"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db9aa986-ac2a-44bb-a88b-04c5d0d502b2.jpg?1767749668"
    }
}
