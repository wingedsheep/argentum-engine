package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reclusive Artificer
 * {2}{U}{R}
 * Creature — Human Artificer
 * 2/3
 * Haste
 * When this creature enters, you may have it deal damage to target creature equal to the number
 * of artifacts you control.
 *
 * The target is chosen as the trigger goes on the stack; the "may" and the artifact count are
 * both settled as it resolves.
 */
val ReclusiveArtificer = card("Reclusive Artificer") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "When this creature enters, you may have it deal damage to target creature equal to the number of artifacts you control."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.DealDamage(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(), t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5299a549-06cf-47e8-b6cd-7e44d9f1efb8.jpg?1783938313"
        ruling("2015-06-22", "Count the number of artifacts you control as Reclusive Artificer's triggered ability resolves to determine how much damage it deals.")
        ruling("2015-06-22", "If you are the only player who controls a creature when Reclusive Artificer's second ability triggers, you must choose one of those creatures as the target, although you can choose to not deal it damage.")
    }
}
