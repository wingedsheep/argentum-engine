package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Promise of Loyalty
 * {4}{W}
 * Sorcery
 *
 * Each player puts a vow counter on a creature they control and sacrifices the rest. Each of
 * those creatures can't attack you or planeswalkers you control for as long as it has a vow
 * counter on it.
 *
 * Each player picks their keeper inside `forEachPlayerCollecting`; the sacrifices and the vows
 * then happen once, outside the loop, so every "rest" is sacrificed at the same time. The
 * restriction is granted per creature with the *caster* as "you" ([CantAttackYou] reads the
 * granting effect's controller), and `WhileAffectedHasCounter(VOW)` ends it for good once the
 * counter leaves — the caster's own keeper gets a vow too, which restricts nothing.
 */
val PromiseOfLoyalty = card("Promise of Loyalty") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player puts a vow counter on a creature they control and sacrifices the rest. " +
        "Each of those creatures can't attack you or planeswalkers you control for as long as it has " +
        "a vow counter on it."

    spell {
        effect = Effects.Pipeline {
            val (kept, rest) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                val (keeper, others) = chooseExactlySplit(
                    1,
                    from = creatures,
                    selectedLabel = "Vow",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose a creature to put a vow counter on. The rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(keeper, others)
            }
            run(
                Effects.ForEachInCollection(
                    kept,
                    Effects.AddCounters(CounterType.VOW, 1, EffectTarget.IterationEntity) then
                        Effects.GrantStaticAbility(
                            CantAttackYou(coversPlaneswalkers = true),
                            EffectTarget.IterationEntity,
                            Duration.WhileAffectedHasCounter(CounterType.VOW)
                        )
                )
            )
            sacrifice(rest)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Sara Winters"
        flavorText = "\"It's us against the world. Forever.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc21e7d5-3641-47fe-add0-8becf5173e28.jpg?1783927608"
    }
}
