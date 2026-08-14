package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moon Girl and Devil Dinosaur
 * {1}{G}{U}
 * Legendary Creature — Human Dinosaur Hero
 * 2/2
 *
 * Whenever you draw your second card each turn, until end of turn, Moon Girl and Devil Dinosaur's
 *   base power and toughness become 6/6 and they gain trample.
 * Whenever an artifact you control enters, draw a card. This ability triggers only once each turn.
 *
 *  - **"Whenever you draw your second card each turn"** is [Triggers.NthCardDrawn]`(2)` — the
 *    controller-scoped default. The per-turn draw count lives on `CardsDrawnThisTurnComponent`
 *    and is reset each turn, so the ability fires at most once a turn even though it carries no
 *    `oncePerTurn` flag; a single multi-card draw that crosses the threshold fires it once
 *    (CR 121.2), and putting cards into hand without the word "draw" (CR 121.5) never advances it.
 *  - **"base power and toughness become 6/6"** is [Effects.SetBasePowerAndToughness] — a Layer 7b
 *    *set* effect, not a `+X/+X` modification — so it overwrites the printed 2/2 while later
 *    +1/+1 counters and pump spells still apply on top (Layers 7c/7d). It targets
 *    [EffectTarget.Self] and expires at end of turn, and it re-applies cleanly if the ability
 *    somehow resolves twice.
 *  - "and they gain trample" rides the same resolution as a turn-scoped [Effects.GrantKeyword],
 *    chained with `then` so both halves land together (or neither, if the source has left).
 *  - **"This ability triggers only once each turn"** on the artifact payoff is `oncePerTurn` on
 *    the triggered ability — the flag suppresses the *trigger*, not just the resolution, so the
 *    second artifact of the turn never puts an ability on the stack at all. Note the two halves
 *    interlock: that draw can itself be your second card of the turn and turn on the 6/6.
 */
val MoonGirlAndDevilDinosaur = card("Moon Girl and Devil Dinosaur") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Dinosaur Hero"
    power = 2
    toughness = 2
    oracleText = "Whenever you draw your second card each turn, until end of turn, Moon Girl and " +
        "Devil Dinosaur's base power and toughness become 6/6 and they gain trample.\n" +
        "Whenever an artifact you control enters, draw a card. This ability triggers only once " +
        "each turn."

    // Whenever you draw your second card each turn, until end of turn, Moon Girl and Devil
    // Dinosaur's base power and toughness become 6/6 and they gain trample.
    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.SetBasePowerAndToughness(
            power = 6,
            toughness = 6,
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        ).then(
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.EndOfTurn)
        )
        description = "Whenever you draw your second card each turn, until end of turn, Moon Girl " +
            "and Devil Dinosaur's base power and toughness become 6/6 and they gain trample."
    }

    // Whenever an artifact you control enters, draw a card. This ability triggers only once each turn.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            binding = TriggerBinding.ANY,
        )
        oncePerTurn = true
        effect = Effects.DrawCards(1)
        description = "Whenever an artifact you control enters, draw a card. This ability " +
            "triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c6a6d11-45cb-4def-a04b-51b91e1747db.jpg?1783902900"
    }
}
