package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Michiko's Reign of Truth // Portrait of Michiko
 * {1}{W} — Enchantment — Saga // Enchantment Creature — Human Noble 0/0
 *
 * Front — Michiko's Reign of Truth:
 *   I, II — Target creature gets +1/+1 until end of turn for each artifact and/or enchantment you control.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Portrait of Michiko:
 *   This creature gets +1/+1 for each artifact and/or enchantment you control.
 *
 * The chapter bonus is counted once on resolution (locked for the turn); the back face's bonus is a
 * live static count. A permanent that is both an artifact and an enchantment counts once.
 */
private val PortraitOfMichiko = card("Portrait of Michiko") {
    manaCost = ""
    colorIndicator = "W"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Noble"
    oracleText = "This creature gets +1/+1 for each artifact and/or enchantment you control."
    power = 0
    toughness = 0

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactOrEnchantment).count(),
            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactOrEnchantment).count(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/back/7/4/74f12c23-5c15-4ae6-8f4d-c5e6c1878817.jpg?1783923921"
    }
}

private val MichikosReignOfTruthFront = card("Michiko's Reign of Truth") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Target creature gets +1/+1 until end of turn for each artifact and/or enchantment you control.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            val creature = target(TargetFilter.Creature)
            val count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactOrEnchantment).count()
            effect = Effects.ModifyStats(count, count, creature)
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Volkan Baǵa"
        flavorText = "Long ago, as the first mortal to bond with Kyodai, Michiko Konda brought an end to " +
            "the Kami War and guided Kamigawa into an age of truth and harmony."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74f12c23-5c15-4ae6-8f4d-c5e6c1878817.jpg?1783923921"
    }
}

val MichikosReignOfTruth: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = MichikosReignOfTruthFront,
    backFace = PortraitOfMichiko,
)
