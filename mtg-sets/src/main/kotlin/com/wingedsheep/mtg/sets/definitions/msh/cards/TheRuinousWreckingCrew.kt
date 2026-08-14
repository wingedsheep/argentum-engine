package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Ruinous Wrecking Crew
 * {X}{B}{R}
 * Legendary Creature — Human Villain
 * 2/2
 *
 * The Ruinous Wrecking Crew enters with X +1/+1 counters on it.
 * When The Ruinous Wrecking Crew enters, choose up to X —
 * • Discard a card, then draw a card.
 * • Target opponent loses 2 life.
 * • Destroy target token.
 * • Each player sacrifices a creature of their choice.
 *
 *  - **Both X clauses read [DynamicAmount.CastX], never `XValue`** — the Goose Mother rule. The
 *    enters-with-counters replacement and the enters trigger are separate resolutions, and the
 *    spell's transient resolution context (which is what `XValue` reads) is already gone by the
 *    time an ETB trigger resolves. `CastX` is the durable, object-scoped reading that rides the
 *    spell's entity onto the battlefield, so the counters and the mode budget are guaranteed to
 *    agree on the same announced X.
 *  - **"Choose up to X"** with a runtime cap is [ModalEffect.chooseUpToDynamic] (Riku of
 *    Many Paths / Bumi, King of Three Trials): the minimum is 0, so X = 0 legally chooses nothing
 *    and the Crew simply enters as a 2/2; the effective maximum is `min(X, 4)` since modes can't
 *    repeat (`allowRepeat = false`, matching the printed card's lack of a "you may choose the same
 *    mode more than once" rider).
 *  - Each mode declares its own mode-local target, so targets are only demanded for the modes
 *    actually chosen (CR 601.2c / 700.2). "Destroy target token" is `Permanent.token()` — *any*
 *    token permanent, not just creature tokens, which is what the unqualified wording means.
 *  - "Discard a card, then draw a card" is a plain sequence, deliberately **not**
 *    `Patterns.Hand.rummage`: the printed text says "then", not "if you do", so an empty hand
 *    still draws.
 *  - "Each player sacrifices a creature of their choice" is the standard edict shape — each
 *    player picks their own, so hexproof/shroud/protection are irrelevant and the Crew's
 *    controller sacrifices too.
 */
val TheRuinousWreckingCrew = card("The Ruinous Wrecking Crew") {
    manaCost = "{X}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Villain"
    power = 2
    toughness = 2
    oracleText = "The Ruinous Wrecking Crew enters with X +1/+1 counters on it.\n" +
        "When The Ruinous Wrecking Crew enters, choose up to X —\n" +
        "• Discard a card, then draw a card.\n" +
        "• Target opponent loses 2 life.\n" +
        "• Destroy target token.\n" +
        "• Each player sacrifices a creature of their choice."

    // The Ruinous Wrecking Crew enters with X +1/+1 counters on it.
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.CastX))

    // When The Ruinous Wrecking Crew enters, choose up to X — …
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseUpToDynamic(
            dynamicMax = DynamicAmount.CastX,
            // Mode 1 — rummage, unconditionally ("then", not "if you do").
            Mode.noTarget(
                Patterns.Hand.discardCards(1).then(Effects.DrawCards(1)),
                description = "Discard a card, then draw a card."
            ),
            // Mode 2 — target opponent loses 2 life.
            Mode.withTarget(
                Effects.LoseLife(2, EffectTarget.ContextTarget(0)),
                Targets.Opponent,
                description = "Target opponent loses 2 life."
            ),
            // Mode 3 — destroy target token (any token permanent).
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                TargetObject(filter = TargetFilter(GameObjectFilter.Permanent.token())),
                description = "Destroy target token."
            ),
            // Mode 4 — edict on every player, each choosing their own creature.
            Mode.noTarget(
                Effects.Sacrifice(
                    GameObjectFilter.Creature,
                    count = 1,
                    target = EffectTarget.PlayerRef(Player.Each),
                ),
                description = "Each player sacrifices a creature of their choice."
            ),
        )
        description = "When The Ruinous Wrecking Crew enters, choose up to X — " +
            "• Discard a card, then draw a card. • Target opponent loses 2 life. " +
            "• Destroy target token. • Each player sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d8c8ceb-84cd-46d2-9230-ab6ca4569334.jpg?1783902898"
    }
}
