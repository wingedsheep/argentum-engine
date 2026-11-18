package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Strength from the Fallen
 * {1}{G}
 * Enchantment
 *
 * Constellation — Whenever this enchantment or another enchantment you control enters, target creature gets +X/+X until end of turn, where X is the number of creature cards in your graveyard.
 */
val StrengthFromTheFallen = card("Strength from the Fallen") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Constellation — Whenever this enchantment or another enchantment you control enters, target creature gets +X/+X until end of turn, where X is the number of creature cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        val x = DynamicAmounts.creatureCardsInYourGraveyard()
        effect = Effects.ModifyStats(x, x, creature)
        description = "Constellation — Whenever this enchantment or another enchantment you control enters, " +
            "target creature gets +X/+X until end of turn, where X is the number of creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c76b3a8e-3cbb-47c4-9535-dfdeece7ee02.jpg?1783939407"
    }
}
