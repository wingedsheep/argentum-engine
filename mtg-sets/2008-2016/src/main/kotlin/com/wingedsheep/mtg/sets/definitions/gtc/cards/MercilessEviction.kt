package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merciless Eviction
 * {4}{W}{B}
 * Sorcery
 * Choose one —
 * • Exile all artifacts.
 * • Exile all creatures.
 * • Exile all enchantments.
 * • Exile all planeswalkers.
 */
val MercilessEviction = card("Merciless Eviction") {
    manaCost = "{4}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Exile all artifacts.\n" +
        "• Exile all creatures.\n" +
        "• Exile all enchantments.\n" +
        "• Exile all planeswalkers."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(exileAll(GameObjectFilter.Artifact), "Exile all artifacts"),
            Mode.noTarget(exileAll(GameObjectFilter.Creature), "Exile all creatures"),
            Mode.noTarget(exileAll(GameObjectFilter.Enchantment), "Exile all enchantments"),
            Mode.noTarget(exileAll(GameObjectFilter.Planeswalker), "Exile all planeswalkers")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Richard Wright"
        flavorText = "\"I once saw the Obzedat moved to action. Since that day, I've been thankful that they're mainly lazy, and dead.\"\n—Aurelia, to Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9876a4c-714b-47e5-9589-148a623af96a.jpg"
    }
}

private fun exileAll(filter: GameObjectFilter) = Effects.ForEachInGroup(
    filter = GroupFilter(filter),
    effect = Effects.Move(EffectTarget.IterationEntity, Zone.EXILE)
)
