package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smoldering Egg // Ashmouth Dragon (Innistrad: Midnight Hunt)
 * {1}{R} Creature — Dragon Egg 0/4 // Creature — Dragon 4/4 (transform)
 *
 * Front — Smoldering Egg
 *   Defender
 *   Whenever you cast an instant or sorcery spell, put a number of ember counters on this creature
 *   equal to the amount of mana spent to cast that spell. Then if this creature has seven or more
 *   ember counters on it, remove them and transform this creature.
 *
 * Back — Ashmouth Dragon
 *   Flying
 *   Whenever you cast an instant or sorcery spell, this creature deals 2 damage to any target.
 *
 * The count is [DynamicAmounts.manaSpentOnTriggeringSpell] — the mana actually paid, so a spell
 * cast without paying its mana cost adds none (as Manaform Hellkite) — and the countdown is Heirloom
 * Mirror's shape over the passive [CounterType.EMBER]: "remove them" clears every ember counter.
 */

private const val EGG_TEXT = "Whenever you cast an instant or sorcery spell, put a number of ember counters on " +
    "this creature equal to the amount of mana spent to cast that spell. Then if this creature has seven or " +
    "more ember counters on it, remove them and transform this creature."

private val SmolderingEggFront = card("Smoldering Egg") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Dragon Egg"
    power = 0
    toughness = 4
    oracleText = "Defender\n$EGG_TEXT"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddDynamicCounters(
            CounterType.EMBER,
            DynamicAmounts.manaSpentOnTriggeringSpell(),
            EffectTarget.Self,
        ) then Effects.If(
            condition = Conditions.SourceCounterCountAtLeast(CounterType.EMBER, 7),
            then = Effects.RemoveAllCountersOfType(CounterType.EMBER, EffectTarget.Self) then
                Effects.Transform(EffectTarget.Self),
        )
        description = EGG_TEXT
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41b6381f-4ff8-49e9-bf00-cfe32851318b.jpg?1783925599"
    }
}

private val AshmouthDragon = card("Ashmouth Dragon") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever you cast an instant or sorcery spell, this creature deals 2 damage to any target."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(2, anyTarget)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/back/4/1/41b6381f-4ff8-49e9-bf00-cfe32851318b.jpg?1783925599"
    }
}

val SmolderingEgg: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SmolderingEggFront,
    backFace = AshmouthDragon,
)
