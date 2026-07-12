package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fear of Death
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, mill two cards.
 * Enchanted creature gets -X/-0, where X is the number of cards in your graveyard.
 *
 * ETB self-mill (Aura enters → mill 2) plus a continuous debuff scoped to the attached creature.
 * The power penalty is a negated dynamic count of cards in your graveyard
 * ([DynamicAmount.Multiply] of [DynamicAmounts.cardsInYourGraveyard] by -1); toughness bonus is a
 * fixed 0 (the Exotic Curse dynamic-aura idiom).
 */
val FearOfDeath = card("Fear of Death") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, mill two cards. (Put the top two cards of your library into your " +
        "graveyard.)\n" +
        "Enchanted creature gets -X/-0, where X is the number of cards in your graveyard."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.mill(2)
    }

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmount.Multiply(DynamicAmounts.cardsInYourGraveyard(), -1),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Anato Finnstark"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b81704d2-d555-4894-b36c-6b65d1ebe681.jpg?1782703150"
    }
}
