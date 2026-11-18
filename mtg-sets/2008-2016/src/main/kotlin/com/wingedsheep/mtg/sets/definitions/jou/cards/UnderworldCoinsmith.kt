package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Underworld Coinsmith
 * {W}{B}
 * Enchantment Creature — Human Cleric
 * 2/2
 *
 * Constellation — Whenever this creature or another enchantment you control enters, you gain 1 life.
 * {W}{B}, Pay 1 life: Each opponent loses 1 life.
 */
val UnderworldCoinsmith = card("Underworld Coinsmith") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, you gain 1 life.\n" +
        "{W}{B}, Pay 1 life: Each opponent loses 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.GainLife(1)
        description = "Constellation — Whenever this creature or another enchantment you control enters, you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{B}"), Costs.PayLife(1))
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{W}{B}, Pay 1 life: Each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Mark Winters"
        flavorText = "Coins of the Underworld are shaped from clay funerary masks."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea5346e3-3fe4-47ea-a184-e237091cc4a1.jpg?1783939400"
    }
}
