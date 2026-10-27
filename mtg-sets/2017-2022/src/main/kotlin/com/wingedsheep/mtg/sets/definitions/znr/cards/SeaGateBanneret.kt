package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sea Gate Banneret — Zendikar Rising #36
 * {W} · Creature — Kor Warrior · 1/2
 *
 * {4}{W}: Creatures you control get +1/+1 until end of turn.
 */
val SeaGateBanneret = card("Sea Gate Banneret") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Warrior"
    oracleText = "{4}{W}: Creatures you control get +1/+1 until end of turn."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
        description = "{4}{W}: Creatures you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Jesper Ejsing"
        flavorText = "The Sea Gate Expeditionary House was founded after the discovery of the Skyclave over Tazeem. Now its banners fly over every corner of Zendikar."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ae4634e-1487-4b66-8f70-bcb4b1fa6e77.jpg"
    }
}
