package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Waltz of Rage
 * {3}{R}{R}
 * Sorcery
 * Target creature you control deals damage equal to its power to each other creature.
 * Until end of turn, whenever a creature you control dies, exile the top card of your library.
 * You may play it until the end of your next turn.
 *
 * Two clauses:
 *  - The chosen creature deals damage equal to its power to each OTHER creature: a
 *    [ForEachInGroupEffect] over [GroupFilter.AllCreatures] with `.otherThanTarget()` (excludes the
 *    chosen creature), each iterated creature ([EffectTarget.Self]) taking [DynamicAmounts.targetPower]
 *    damage *from the chosen creature itself* (`damageSource = ContextTarget(0)` — so combat keywords
 *    like deathtouch / lifelink on the chosen creature apply, and "dealt damage by" triggers see the
 *    correct source).
 *  - A turn-duration event-based delayed trigger ([CreateDelayedTriggerEffect] with
 *    [Triggers.YourCreatureDies], `expiry = EndOfTurn`, `fireOnce = false`) that fires once per
 *    creature-you-control death this turn, impulse-drawing the top card with
 *    [MayPlayExpiry.UntilEndOfNextTurn]. The per-creature [Triggers.YourCreatureDies] (ZoneChangeEvent,
 *    ANY binding) matches the singular "a creature" wording — a board wipe fires it once per creature.
 */
val WaltzOfRage = card("Waltz of Rage") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to each other creature. " +
        "Until end of turn, whenever a creature you control dies, exile the top card of your library. " +
        "You may play it until the end of your next turn."

    spell {
        val chosen = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            // Target creature you control deals damage equal to its power to each other creature.
            ForEachInGroupEffect(
                filter = GroupFilter(GameObjectFilter.Creature).otherThanTarget(),
                effect = DealDamageEffect(
                    amount = DynamicAmounts.targetPower(0),
                    target = EffectTarget.Self,
                    damageSource = chosen
                )
            ),
            // Until end of turn, whenever a creature you control dies, exile the top card of your
            // library. You may play it until the end of your next turn.
            CreateDelayedTriggerEffect(
                trigger = Triggers.YourCreatureDies,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                fireOnce = false,
                effect = Effects.Composite(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                        storeAs = "waltzExiled"
                    ),
                    MoveCollectionEffect(
                        from = "waltzExiled",
                        destination = CardDestination.ToZone(Zone.EXILE)
                    ),
                    GrantMayPlayFromExileEffect("waltzExiled", MayPlayExpiry.UntilEndOfNextTurn)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abf17d8b-12bc-4122-865d-50cf91f04f67.jpg?1726286472"
    }
}
