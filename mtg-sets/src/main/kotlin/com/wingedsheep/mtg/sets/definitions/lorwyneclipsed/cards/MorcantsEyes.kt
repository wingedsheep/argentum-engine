package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Morcant's Eyes
 * {1}{G}
 * Kindred Enchantment — Elf
 *
 * At the beginning of your upkeep, surveil 1.
 * {4}{G}{G}, Sacrifice this enchantment: Create X 2/2 black and green Elf creature
 * tokens, where X is the number of Elf cards in your graveyard. Activate only as a
 * sorcery.
 */
val MorcantsEyes = card("Morcant's Eyes") {
    manaCost = "{1}{G}"
    typeLine = "Kindred Enchantment — Elf"
    oracleText = "At the beginning of your upkeep, surveil 1. " +
        "(Look at the top card of your library. You may put it into your graveyard.)\n" +
        "{4}{G}{G}, Sacrifice this enchantment: Create X 2/2 black and green Elf creature tokens, " +
        "where X is the number of Elf cards in your graveyard. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = EffectPatterns.surveil(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{G}"), Costs.SacrificeSelf)
        effect = CreateTokenEffect(
            count = DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype("Elf")),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Elf"),
            imageUri = "https://cards.scryfall.io/normal/front/3/9/39b36f22-21f9-44fe-8a49-bdc859503342.jpg?1767955588"
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a730b254-ff7c-4f89-a559-b44ad7fd6c6c.jpg?1767732849"
        ruling(
            "2025-11-17",
            "The value of X is calculated only once, as Morcant's Eyes's last ability resolves. " +
                "In most cases, Morcant's Eyes will be in your graveyard at that time, so it will count toward the value of X."
        )
    }
}
