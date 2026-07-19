package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.MoveType

/**
 * Break the Spell
 * {W}
 * Instant
 *
 * Destroy target enchantment. If a permanent you controlled or a token was destroyed this way,
 * draw a card.
 *
 * The conditional draw reads characteristics that are gone by the time the draw would happen:
 * `ControllerComponent` is stripped when a permanent leaves the battlefield, and a destroyed token
 * ceases to exist. So the pipeline snapshots the answer *before* the destroy and cross-references
 * it with what actually died (Builder's Bane shape):
 *
 *   1. `gather(ChosenTargets)` — reference the resolved target.
 *   2. `filterSplit(… you control or a token …)` — evaluated while it is still on the battlefield.
 *      Only the *rest* slot is used downstream, as the set to subtract in step 4.
 *   3. `moveTracked(…, MoveType.Destroy)` — the standard destroy path. Indestructible and
 *      regenerated permanents are skipped by the executor and therefore drop out of the tracked
 *      "destroyed" slot, which is exactly the "was destroyed this way" gate (first ruling below).
 *      A permanent whose death is replaced by a move to some other zone still counts as destroyed
 *      and stays in the slot (second ruling).
 *   4. `filter(destroyed, ExcludeOtherCollection(rest))` — set difference, i.e. the intersection of
 *      "actually destroyed" with "was yours or a token". Non-empty ⇒ draw.
 *
 * If the enchantment is an illegal target on resolution the spell is countered by the rules and
 * nothing happens, including the draw.
 */
val BreakTheSpell = card("Break the Spell") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target enchantment. If a permanent you controlled or a token was " +
        "destroyed this way, draw a card."

    spell {
        target("target enchantment", Targets.Enchantment)
        effect = Effects.Pipeline(
            descriptionOverride = "Destroy target enchantment. If a permanent you controlled or " +
                "a token was destroyed this way, draw a card."
        ) {
            val targeted = gather(CardSource.ChosenTargets, name = "breakSpellTarget")

            // "a permanent you controlled or a token" — snapshotted while it is still in play.
            val (_, neither) = filterSplit(
                targeted,
                GameObjectFilter.Any.youControl() or GameObjectFilter.Token,
                name = "breakSpellYoursOrToken",
                restName = "breakSpellNeither"
            )

            // "destroyed this way" — indestructible / regenerated permanents drop out here.
            val destroyed = moveTracked(
                targeted,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy,
                name = "breakSpellDestroyed"
            )

            // destroyed ∖ (neither yours nor a token) = destroyed ∩ (yours or a token)
            val qualifying = filter(
                destroyed,
                CollectionFilter.ExcludeOtherCollection(neither.key),
                name = "breakSpellQualifyingDestroyed"
            )

            ifNotEmpty(qualifying) {
                run(Effects.DrawCards(1))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Miranda Meeks"
        flavorText = "As Hylda watched Ruby drag Kellan's unconscious form through the cold, " +
            "Hylda felt something she hadn't felt in a long time: compassion."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7094fc0-1d26-429c-9b49-37718c4a5c80.jpg?1783915136"

        ruling(
            "2023-09-01",
            "If the enchantment has indestructible, it won't be destroyed. You won't draw a card."
        )
        ruling(
            "2023-09-01",
            "If a permanent you controlled or a token is destroyed with Break the Spell but is put " +
                "into a zone other than a graveyard as a result, you will draw a card."
        )
    }
}
