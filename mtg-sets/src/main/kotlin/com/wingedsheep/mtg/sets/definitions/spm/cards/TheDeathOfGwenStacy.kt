package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * The Death of Gwen Stacy (SPM #54)
 * {2}{B} — Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy target creature.
 * II — Each player may discard a card. Each player who doesn't loses 3 life.
 * III — Exile any number of target players' graveyards.
 *
 * Chapter I is a plain [Effects.Destroy] on target creature (respects indestructible).
 *
 * Chapter II is a *symmetric* per-player choice, so it iterates every player with
 * [ForEachPlayerEffect] over [Player.Each] — inside the loop the iterated player is the
 * controller, so [EffectTarget.Controller] resolves to them for both the discard and the
 * life loss. Each player faces a "may discard a card" yes/no ([Gate.MayDecide]) whose
 * `otherwise` is "lose 3 life" — declining (the "who doesn't") runs the life loss. The
 * [FeasibilityCheck.HasCardsInZone] on the gate makes an empty-handed player skip the
 * pointless prompt and take the 3 life loss directly (they can't discard, so they "don't").
 * The [GatedEffect] is built directly because the `MayEffect` facade doesn't expose the
 * `feasibility` slot needed for that empty-hand branch.
 *
 * Chapter III targets "any number of target players" ([TargetPlayer] `unlimited`) and, per
 * chosen target, exiles that player's whole graveyard — a gather-then-move over
 * [Player.ContextPlayer] `(0)` (the current iterated target) inside [ForEachTargetEffect],
 * the Angel of Finality / Hollow Marauder shape, which handles an empty graveyard cleanly.
 */
val TheDeathOfGwenStacy = card("The Death of Gwen Stacy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy target creature.\n" +
        "II — Each player may discard a card. Each player who doesn't loses 3 life.\n" +
        "III — Exile any number of target players' graveyards."

    // I — Destroy target creature.
    sagaChapter(1) {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Destroy(creature)
    }

    // II — Each player may discard a card. Each player who doesn't loses 3 life.
    sagaChapter(2) {
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                GatedEffect(
                    gate = Gate.MayDecide(
                        feasibility = FeasibilityCheck.HasCardsInZone(Zone.HAND)
                    ),
                    then = Effects.Discard(1, EffectTarget.Controller),
                    otherwise = Effects.LoseLife(3, EffectTarget.Controller),
                    descriptionOverride = "You may discard a card. If you don't, you lose 3 life."
                )
            )
        )
    }

    // III — Exile any number of target players' graveyards.
    sagaChapter(3) {
        target("any number of target players", TargetPlayer(unlimited = true))
        effect = ForEachTargetEffect(
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    storeAs = "gwenTargetGraveyard"
                ),
                MoveCollectionEffect(
                    from = "gwenTargetGraveyard",
                    destination = CardDestination.ToZone(Zone.EXILE)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Bill Sienkiewicz"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/690f1f31-f8c5-4336-9ec9-72ff761e3adc.jpg?1783905346"
    }
}
