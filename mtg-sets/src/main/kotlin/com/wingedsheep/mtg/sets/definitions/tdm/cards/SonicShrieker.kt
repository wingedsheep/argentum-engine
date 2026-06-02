package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sonic Shrieker — Tarkir: Dragonstorm #226
 * {2}{R}{W}{B} · Creature — Dragon · 4/4
 *
 * Flying
 * When this creature enters, it deals 2 damage to any target and you gain 2 life.
 * If a player is dealt damage this way, they discard a card.
 *
 * The ETB resolves as one composite: deal 2 to the chosen "any target" (source defaults to
 * this creature), gain 2 life, then — only if that target was a player — that same player
 * discards a card. The player-only follow-up is gated by [Conditions.TargetIsPlayer] (the
 * companion `TargetMatchesFilter` matches game objects and returns false for a player target,
 * so a dedicated player check is required), and the discard is aimed at the damaged player via
 * [EffectTarget.ContextTarget]. Modeling "is dealt damage this way" as "the target was a player"
 * matches the engine's damage model — a player target always takes the 2 damage absent a
 * player-damage prevention shield, which the engine does not broadly support.
 */
val SonicShrieker = card("Sonic Shrieker") {
    manaCost = "{2}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, it deals 2 damage to any target and you gain 2 life. " +
        "If a player is dealt damage this way, they discard a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.Composite(listOf(
            Effects.DealDamage(2, anyTarget),
            Effects.GainLife(2),
            ConditionalEffect(
                condition = Conditions.TargetIsPlayer(0),
                effect = Effects.Discard(1, EffectTarget.ContextTarget(0))
            )
        ))
        description = "When this creature enters, it deals 2 damage to any target and you gain 2 life. " +
            "If a player is dealt damage this way, they discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Jason A. Engle"
        flavorText = "To ensure a fair competition, the Great Assemblage's war shriek contest has " +
            "two divisions: dragons, and everyone else."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c231437-8bec-42e0-9175-af74c752b119.jpg?1743204895"
    }
}
