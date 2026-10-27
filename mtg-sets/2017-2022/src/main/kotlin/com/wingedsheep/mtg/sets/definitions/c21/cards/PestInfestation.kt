package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pest Infestation — Commander 2021 #65 (canonical printing)
 * {X}{X}{G} · Sorcery
 *
 * Destroy up to X target artifacts and/or enchantments. Create twice X 1/1 black and green Pest
 * creature tokens with "When this token dies, you gain 1 life."
 *
 * Force of Vigor's "up to N target artifacts and/or enchantments" with the cap read from the
 * announced X (`dynamicMaxCount = xValue()`, as March of Swirling Mist), then the predefined Pest
 * token (Tend the Pests) created `xValue() * 2` times — twice X regardless of how many permanents
 * were destroyed (ruling). If targets were chosen and all became illegal, the spell doesn't
 * resolve and no Pests are made (CR 608.2b).
 */
val PestInfestation = card("Pest Infestation") {
    manaCost = "{X}{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy up to X target artifacts and/or enchantments. Create twice X 1/1 black " +
        "and green Pest creature tokens with \"When this token dies, you gain 1 life.\""

    spell {
        targets(
            TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment),
            optional = true,
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0))) then
            Effects.CreatePredefinedToken("Pest", DynamicAmounts.xValue() * 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Brian Valeza"
        flavorText = "A thousand years of history undone by one juvenile prank."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4720b4f2-e6af-4223-9250-a0ed21ed5693.jpg?1792506360"
        ruling("2021-04-16", "The number of Pests created is equal to twice the chosen value of X, no matter how many artifacts and enchantments were destroyed.")
        ruling("2021-04-16", "If any artifacts or enchantments were chosen as targets, and all of them are illegal targets as Pest Infestation tries to resolve, it won't resolve and none of its effects will happen. No Pests will be created.")
    }
}
