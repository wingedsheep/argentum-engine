package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lost in the Maze — Murders at Karlov Manor #64
 * {X}{U}{U} · Enchantment
 *
 * Flash
 * When this enchantment enters, tap X target creatures. Put a stun counter on each of those
 * creatures you don't control.
 * Tapped creatures you control have hexproof.
 *
 * A blue fog that stays on the table. Flashed in before combat it taps the attackers and stuns them
 * so they miss the next untap, and the static half then protects your own tapped blockers — the two
 * halves are asymmetric on purpose, which is why the stun clause is scoped and the hexproof clause is
 * not.
 *
 * "Tap X target creatures" clamps the target count to the X actually paid via `dynamicMaxCount`
 * rather than baking in a number, and `optional = true` carries the fact that X may be 0 (a legal,
 * if pointless, cast).
 *
 * The amount is [DynamicAmount.CastX], **not** `XValue`. The distinction is load-bearing and silent
 * when wrong: `XValue` reads the transient resolution context and is populated only while the spell
 * itself resolves, which is right for an instant like Icy Blast ("tap X target creatures" on the
 * spell) but wrong here — this X is read by an *enters-the-battlefield trigger* on the permanent the
 * spell left behind, after that context is gone. [DynamicAmount.CastX] is the durable, object-scoped
 * reading that rides onto the permanent, so the trigger sees the X the enchantment was cast for.
 *
 * The stun counters go on a *subset* of the chosen targets, not all of them — [ForEachTargetEffect]
 * walks the targets and a [Gate.WhenCondition] per iteration tests that target's controller, so your
 * own creatures get tapped but not stunned. Note this reads "creatures you **don't** control" while
 * the filter says `opponentControls()`: the two coincide in every format the engine currently plays,
 * and the SDK's [GameObjectFilter] controller axis has no "not you" predicate. The distinction would
 * only bite in Two-Headed Giant, where a teammate's creature is neither yours nor an opponent's.
 *
 * The stun counter itself carries its behavior (CR 122.1i / 701.22) — the counter is inherent, so
 * nothing here needs to model the skipped untap.
 *
 * "Tapped creatures you control have hexproof" is a continuous [GrantKeyword] static over
 * `Creature.tapped().youControl()`. The set is recomputed continuously rather than locked in at
 * resolution: a creature that taps later this turn gains hexproof, and untapping loses it.
 */
val LostInTheMaze = card("Lost in the Maze") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, tap X target creatures. Put a stun counter on each of those " +
        "creatures you don't control. (If a permanent with a stun counter would become untapped, " +
        "remove one from it instead.)\n" +
        "Tapped creatures you control have hexproof."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetCreature(optional = true, dynamicMaxCount = DynamicAmount.CastX)
        effect = Effects.TapEachTarget()
            .then(
                ForEachTargetEffect(
                    listOf(
                        GatedEffect(
                            gate = Gate.WhenCondition(
                                Conditions.TargetMatchesFilter(
                                    GameObjectFilter.Creature.opponentControls()
                                )
                            ),
                            then = Effects.AddCounters(
                                Counters.STUN,
                                1,
                                EffectTarget.ContextTarget(0)
                            )
                        )
                    )
                )
            )
        description = "When this enchantment enters, tap X target creatures. Put a stun counter on " +
            "each of those creatures you don't control."
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter(GameObjectFilter.Creature.tapped().youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6308dc62-d945-4761-aa4c-ef8e9271e901.jpg?1783912909"
    }
}
