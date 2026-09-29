package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Orthion, Hero of Lavabrink
 * {3}{R}
 * Legendary Creature — Human Soldier
 * 3/3
 * {1}{R}, {T}: Create a token that's a copy of another target creature you control. It gains haste.
 * Sacrifice it at the beginning of the next end step. Activate only as a sorcery.
 * {6}{R}{R}{R}, {T}: Create five tokens that are copies of another target creature you control.
 * They gain haste. Sacrifice them at the beginning of the next end step. Activate only as a sorcery.
 *
 * Haste rides on the copy via `addedKeywords` (the Molten Duplication / Kiki-Jiki modelling); the
 * tokens are sacrificed at the next end step via `sacrificeAtStep = Step.END`.
 */
val OrthionHeroOfLavabrink = card("Orthion, Hero of Lavabrink") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "{1}{R}, {T}: Create a token that's a copy of another target creature you control. It gains haste. " +
        "Sacrifice it at the beginning of the next end step. Activate only as a sorcery.\n" +
        "{6}{R}{R}{R}, {T}: Create five tokens that are copies of another target creature you control. They gain haste. " +
        "Sacrifice them at the beginning of the next end step. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()).other())
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            addedKeywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
        )
        description = "{1}{R}, {T}: Create a token that's a copy of another target creature you control. It gains haste. " +
            "Sacrifice it at the beginning of the next end step. Activate only as a sorcery."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}{R}{R}{R}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()).other())
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            count = 5,
            addedKeywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
        )
        description = "{6}{R}{R}{R}, {T}: Create five tokens that are copies of another target creature you control. " +
            "They gain haste. Sacrifice them at the beginning of the next end step. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "334"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71dadbb3-7b8a-4656-973f-65a3284afe07.jpg?1783916901"
        ruling("2023-04-14", "The tokens each copy exactly what was printed on the original creature and nothing else (unless that creature is copying something else or is a token). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2023-04-14", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2023-04-14", "If the copied creature is copying something else, then the token enters the battlefield as whatever that creature copied.")
        ruling("2023-04-14", "If the copied creature is a token, the token that's created copies the original characteristics of that token as stated by the effect that created that token.")
        ruling("2023-04-14", "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters the battlefield. Any \"as [this creature] enters the battlefield\" or \"[this creature] enters the battlefield with\" abilities of the copied creature will also work.")
    }
}
