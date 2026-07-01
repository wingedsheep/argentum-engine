package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bumi, King of Three Trials
 * {5}{G}
 * Legendary Creature — Human Noble Ally
 * 4/4
 *
 * When Bumi enters, choose up to X, where X is the number of Lesson cards in your graveyard —
 * • Put three +1/+1 counters on Bumi.
 * • Target player scries 3.
 * • Earthbend 3. (Target land you control becomes a 0/0 creature with haste that's still a land.
 *   Put three +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped.)
 *
 * Implementation:
 * - The novelty is a modal "choose up to X" whose cap X is a [DynamicAmount] resolved when the ETB
 *   trigger resolves — not a fixed literal. [ModalEffect.chooseUpToDynamic] already models exactly
 *   this (Riku of Many Paths): `minChooseCount` is treated as 0 (declining all picks is legal,
 *   matching "up to"), the effective maximum is `min(eval, modes.size)`, and modes can't repeat
 *   (`allowRepeat = false`). Here the cap is `DynamicAmount.Count` of Lesson cards in your graveyard
 *   — the same dynamic Serpent of the Pass uses — so with zero Lessons no mode may be chosen and
 *   Bumi simply enters.
 * - The three modes reuse existing effects, each declaring its own (mode-local) target so the target
 *   is only demanded when that mode is chosen (CR 601.2c / 700.2 — modes choose targets per chosen
 *   mode):
 *     • [Effects.AddCounters] of three +1/+1 counters on Bumi ([EffectTarget.Self], no target).
 *     • [Effects.Scry]`(3, target)` — the new player-scoped scry; the target player scries their own
 *       library. The player is the mode-local [EffectTarget.ContextTarget].
 *     • [Effects.Earthbend]`(3, target)` — the TLA keyword action over a "target land you control".
 */
val BumiKingOfThreeTrials = card("Bumi, King of Three Trials") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 4
    toughness = 4
    oracleText = "When Bumi enters, choose up to X, where X is the number of Lesson cards in your " +
        "graveyard —\n" +
        "• Put three +1/+1 counters on Bumi.\n" +
        "• Target player scries 3.\n" +
        "• Earthbend 3. (Target land you control becomes a 0/0 creature with haste that's still a " +
        "land. Put three +1/+1 counters on it. When it dies or is exiled, return it to the " +
        "battlefield tapped.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseUpToDynamic(
            dynamicMax = DynamicAmount.Count(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter.Any.withSubtype(Subtype.LESSON)
            ),
            // Mode 1 — three +1/+1 counters on Bumi.
            Mode.noTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self),
                description = "Put three +1/+1 counters on Bumi."
            ),
            // Mode 2 — target player scries 3.
            Mode.withTarget(
                Effects.Scry(3, EffectTarget.ContextTarget(0)),
                Targets.Player,
                description = "Target player scries 3."
            ),
            // Mode 3 — Earthbend 3.
            Mode.withTarget(
                Effects.Earthbend(3, EffectTarget.ContextTarget(0)),
                TargetObject(filter = TargetFilter.Land.youControl()),
                description = "Earthbend 3. (Target land you control becomes a 0/0 creature with " +
                    "haste that's still a land. Put three +1/+1 counters on it. When it dies or is " +
                    "exiled, return it to the battlefield tapped.)"
            )
        )
        description = "When Bumi enters, choose up to X, where X is the number of Lesson cards in " +
            "your graveyard — • Put three +1/+1 counters on Bumi. • Target player scries 3. " +
            "• Earthbend 3."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Thomas Chamberlain-Keen"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a268697b-22b0-4e1b-a5b6-d9be95025e57.jpg?1764121152"
    }
}
