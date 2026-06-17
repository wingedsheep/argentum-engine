package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Spiteful Banditry
 * {X}{R}{R}
 * Enchantment
 *
 * When this enchantment enters, it deals X damage to each creature.
 * Whenever one or more creatures your opponents control die, you create a Treasure token.
 * This ability triggers only once each turn.
 */
val SpitefulBanditry = card("Spiteful Banditry") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, it deals X damage to each creature.\n" +
        "Whenever one or more creatures your opponents control die, you create a Treasure token. This ability triggers only once each turn."

    // When this enchantment enters, it deals X damage to each creature.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)
        )
    }

    // Whenever one or more creatures your opponents control die, you create a Treasure token.
    // This ability triggers only once each turn. Batched (fires once per death event), so a
    // board wipe that kills several of an opponent's creatures still makes a single Treasure.
    triggeredAbility {
        trigger = Triggers.OneOrMoreCreaturesAnOpponentControlsDie()
        oncePerTurn = true
        effect = Effects.CreateTreasure(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Manuel Castañón"
        flavorText = "\"One ill turn deserves another.\"\n—Saruman"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e85a82bc-49f3-4694-b688-808a541146db.jpg?1686969184"
    }
}
