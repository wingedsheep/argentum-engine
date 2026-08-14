package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Alley Assailant — Murders at Karlov Manor #76
 * {2}{B} · Creature — Vampire Rogue · 3/3
 *
 * This creature enters tapped.
 * Disguise {4}{B}{B}
 * When this creature is turned face up, target opponent loses 3 life and you gain 3 life.
 *
 * The two lines are in tension by design, and the tension is the card. Hard-cast for {2}{B} it is a
 * tapped 3/3 with no payoff — the drain only ever comes off the *disguise* route (CR 702.168d:
 * turning face up is not entering the battlefield, so an enters trigger would never fire, and this
 * one isn't an enters trigger to begin with).
 *
 * "Enters tapped" is a self-replacement (`EntersTapped`) carried by the card's own abilities, so
 * casting it face down dodges it entirely: a face-down permanent has no abilities at all
 * (CR 702.168a / 708.2), so the 2/2 with ward {2} arrives untapped and can attack the turn it comes
 * down given haste, or block immediately. Flipping it later for {4}{B}{B} likewise leaves its
 * tapped status alone (CR 701.34c), so the 3/3 that appears mid-combat is untapped.
 *
 * The drain is two independent fixed amounts, not a linked `DrainLife`: the opponent loses 3 and
 * you gain 3 regardless of how much life they actually had to lose.
 */
val AlleyAssailant = card("Alley Assailant") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "This creature enters tapped.\n" +
        "Disguise {4}{B}{B} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, target opponent loses 3 life and you gain 3 life."
    power = 3
    toughness = 3
    disguise = "{4}{B}{B}"

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val victim = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            Effects.LoseLife(3, target = victim),
            Effects.GainLife(3)
        )
        description = "When this creature is turned face up, target opponent loses 3 life and you gain 3 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edf238c9-61de-4f3a-b82f-05af46e5e81b.jpg?1783912903"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Turning a permanent face up or face down doesn't change whether that permanent is " +
                "tapped or untapped."
        )
    }
}
