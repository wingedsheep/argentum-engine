package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rionya, Fire Dancer — Commander 2021 #55
 * {3}{R}{R} · Legendary Creature — Human Wizard · 3/4
 *
 * At the beginning of combat on your turn, create X tokens that are copies of another target
 * creature you control, where X is one plus the number of instant and sorcery spells you've cast
 * this turn. They gain haste. Exile them at the beginning of the next end step.
 *
 * X is read as the trigger resolves (`SpellsCastThisTurn` over instants and sorceries), so spells
 * cast in response to the trigger count (ruling). "They gain haste" is a non-copy effect granted
 * after creation through the [CollectionSlot.CreatedTokens] pipeline — not a copiable value baked
 * into the tokens — and with no stated duration it lasts as long as the tokens do. The tokens are
 * exiled (not sacrificed) at the next end step via `exileAtStep = Step.END`.
 */
val RionyaFireDancer = card("Rionya, Fire Dancer") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "At the beginning of combat on your turn, create X tokens that are copies of another " +
        "target creature you control, where X is one plus the number of instant and sorcery spells " +
        "you've cast this turn. They gain haste. Exile them at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()).other())
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            count = 1 + DynamicAmounts.spellsCastThisTurn(filter = GameObjectFilter.InstantOrSorcery),
            exileAtStep = Step.END,
        ) then
            Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent),
            )
        description = "At the beginning of combat on your turn, create X tokens that are copies of another " +
            "target creature you control, where X is one plus the number of instant and sorcery spells " +
            "you've cast this turn. They gain haste. Exile them at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1e92b9c-4234-436d-a741-37c5e45859e5.jpg?1783927591"
        ruling("2021-04-16", "The triggered ability counts all instants and sorceries that were cast before it resolves. If you cast instant spells in response to the ability, those spells will count.")
        ruling("2021-04-16", "The tokens copy exactly what is printed on the creature and nothing else (unless that creature is a token or is copying something else; see below). They don't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on.")
        ruling("2021-04-16", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2021-04-16", "If the copied creature is copying something else, the tokens enter the battlefield as whatever that creature is copying.")
        ruling("2021-04-16", "If the copied creature is itself a token, the tokens created by Rionya copy the original characteristics of that token as stated by the effect that created it.")
        ruling("2021-04-16", "Any enters-the-battlefield abilities of the copied creature will trigger when the tokens enter the battlefield. Any \"As [this creature] enters the battlefield\" or \"[This creature] enters the battlefield with\" abilities of the copied creature will also work.")
    }
}
