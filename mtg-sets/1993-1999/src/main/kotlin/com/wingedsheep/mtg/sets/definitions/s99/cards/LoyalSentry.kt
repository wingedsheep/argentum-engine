package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val LoyalSentry = card("Loyal Sentry") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "When this creature blocks a creature, destroy that creature and this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.blocks(GameObjectFilter.Creature)
        effect = Effects.Destroy(EffectTarget.TriggeringEntity) then
            Effects.Destroy(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e50b20e6-5853-405e-b2f5-3e302dc7103f.jpg?1783946049"
        ruling("2018-03-16", "Loyal Sentry and the creature it blocks are destroyed before combat damage is dealt. The blocked creature is destroyed even if Loyal Sentry leaves the battlefield before its triggered ability resolves.")
    }
}
