package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arena Trickster
 * {3}{R}
 * Creature — Human Shaman
 * 3/3
 * Whenever you cast your first spell during each opponent's turn, put a +1/+1 counter on this
 * creature.
 *
 * Stinging Lionfish's trigger: `Triggers.you.castsNth(1)` (the caster's per-turn cast count, so a
 * spell cast before the Trickster arrived still uses up the "first", per the ruling) restricted by
 * [Conditions.IsOpponentsTurn] — once per opponent's turn.
 */
val ArenaTrickster = card("Arena Trickster") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast your first spell during each opponent's turn, put a +1/+1 counter " +
        "on this creature."

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        triggerRestriction = Conditions.IsOpponentsTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast your first spell during each opponent's turn, put a +1/+1 " +
            "counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "PINDURSKI"
        flavorText = "\"It's not enough to surprise your opponent. You have to delight the crowd as well.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e096049d-9f02-4abb-bb5a-97b14fd17099.jpg?1783931556"
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "This ability triggers only on your very first spell during an opponent's turn, not the first spell after the card is on the battlefield. If you cast a spell before it's on the battlefield (including if you cast this card somehow during an opponent's turn), the ability won't trigger.")
        ruling("2020-01-24", "If you have more than one opponent, this ability can trigger once during each of those opponents' turns.")
        ruling("2020-01-24", "In a Two-Headed Giant game, this ability triggers no more than once during each opposing team's turn.")
    }
}
