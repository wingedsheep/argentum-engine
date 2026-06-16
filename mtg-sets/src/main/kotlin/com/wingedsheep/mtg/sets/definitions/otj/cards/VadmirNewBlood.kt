package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vadmir, New Blood
 * {1}{B}
 * Legendary Creature — Vampire Rogue
 * 2/2
 *
 * Whenever you commit a crime, put a +1/+1 counter on Vadmir. This ability triggers only once each turn.
 * As long as Vadmir has four or more +1/+1 counters on it, it has menace and lifelink.
 *
 * The crime trigger is the same once-per-turn `+1/+1` self-counter shape as Blood Hustler. The
 * keyword grants are two [ConditionalStaticAbility]s wrapping `GrantKeyword(kw, Filters.Self)`, gated by
 * [Conditions.SourceCounterCountAtLeast]`(+1/+1, 4)` — re-evaluated from projected state, so
 * menace/lifelink appear the moment the fourth counter lands and vanish if counters are removed below
 * the threshold.
 */
val VadmirNewBlood = card("Vadmir, New Blood") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Rogue"
    power = 2
    toughness = 2
    oracleText = "Whenever you commit a crime, put a +1/+1 counter on Vadmir. This ability triggers " +
        "only once each turn. (Targeting opponents, anything they control, and/or cards in their " +
        "graveyards is a crime.)\n" +
        "As long as Vadmir has four or more +1/+1 counters on it, it has menace and lifelink."

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = AddCountersEffect(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = EffectTarget.Self
        )
        description = "Whenever you commit a crime, put a +1/+1 counter on Vadmir. This ability " +
            "triggers only once each turn."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.MENACE, Filters.Self),
            condition = Conditions.SourceCounterCountAtLeast(Counters.PLUS_ONE_PLUS_ONE, 4)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, Filters.Self),
            condition = Conditions.SourceCounterCountAtLeast(Counters.PLUS_ONE_PLUS_ONE, 4)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/828b5855-af5a-46a6-8fd4-0a2e28f3bb01.jpg?1712355705"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
        ruling("2024-04-12", "A player can commit only one crime per spell or ability they control. Targeting multiple opponents, permanents, spells, abilities, and/or cards with the same spell or ability doesn't constitute committing multiple crimes.")
    }
}
