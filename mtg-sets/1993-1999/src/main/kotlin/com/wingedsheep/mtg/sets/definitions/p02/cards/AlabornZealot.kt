package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Alaborn Zealot
 * {W}
 * Creature — Human Soldier
 * 1/1
 *
 * When this creature blocks a creature, destroy both creatures.
 *
 * `Triggers.self.blocks(Creature)` fires once per blocked attacker with that attacker as the
 * triggering entity; the destroy pair is Self + TriggeringEntity (Dead-Iron Sledge idiom).
 */
val AlabornZealot = card("Alaborn Zealot") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "When this creature blocks a creature, destroy both creatures."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.blocks(GameObjectFilter.Creature)
        effect = Effects.Destroy(EffectTarget.Self) then
            Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "David Horne"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5eba273-0b83-42a7-b8b0-9e0cd6a7aa6f.jpg?1783946495"
    }
}
