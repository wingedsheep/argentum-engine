package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Frantic Scapegoat — Murders at Karlov Manor #126
 * {R} · Creature — Goat · 1/1 · Uncommon
 *
 * Haste
 * When this creature enters, suspect it. (It has menace and can't block.)
 * Whenever one or more other creatures you control enter, if this creature is suspected, you may
 * suspect one of the other creatures. If you do, this creature is no longer suspected.
 *
 * A one-mana hasty 2-power attacker (menace, from being suspected) that pays for the drawback by
 * *passing the suspicion along* — every subsequent creature you deploy can take the blame, leaving
 * the Goat a plain 1/1 that blocks again while the new creature gains menace on offence.
 *
 * Three details the wording pins down, and how each is modelled:
 *
 * - **"one or more other creatures you control enter"** is a batching trigger (CR 603.6a): a mass
 *   reanimation that returns four creatures fires this once, not four times.
 *   [Triggers.OneOrMorePermanentsEnter] with `excludeSource = true` is exactly that shape — the
 *   filter's default controller scope is "you control", and `excludeSource` realises "other" so the
 *   Goat's own entry can never feed its second ability. The matching members of the batch are
 *   seeded into the resolving trigger's pipeline as
 *   [IterationSpace.TRIGGER_CAPTURED_COLLECTION], which is what makes "one of the **other**
 *   creatures" a closed, well-defined set rather than a fresh board-wide target search.
 *
 * - **"if this creature is suspected"** is an intervening-if (CR 603.4), so it is a
 *   `triggerCondition` ([Conditions.SourceIsSuspected]) rather than a gate inside the effect: the
 *   ability doesn't go on the stack at all while the Goat is unsuspected, and it is re-checked on
 *   resolution. Once the suspicion has been handed off, later creatures entering stop triggering
 *   it entirely — which is the card's actual rate limit.
 *
 * - **"you may … If you do"** is a single optional choice with a linked consequence, not two
 *   independent ones. Modelled as a [SelectFromCollectionEffect] with
 *   [SelectionMode.ChooseUpTo] 1 over the captured batch (declining selects zero), then a
 *   [ForEachInCollectionEffect] that suspects whatever was picked, then a
 *   [Conditions.CollectionContainsMatch]-gated [Effects.NoLongerSuspected] on the Goat. Gating the
 *   un-suspect on the *selection* rather than on the "may" is what makes a declined choice leave
 *   the Goat suspected, per the "If you do".
 *
 * `useTargetingUI` puts the pick on the battlefield rather than in an overlay — the candidates are
 * permanents already in play, and which duplicate token is which matters here.
 *
 * Note the two halves are genuinely independent statuses, not a transfer primitive: per the printed
 * rulings suspecting an already-suspected creature does nothing, and there is no limit on how many
 * creatures are suspected at once. Composing [Effects.Suspect] and [Effects.NoLongerSuspected]
 * keeps both facts true — pointing the Goat at an already-suspected creature still clears the
 * Goat's own suspicion, exactly as the card reads.
 */
val FranticScapegoat = card("Frantic Scapegoat") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goat"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "When this creature enters, suspect it. (It has menace and can't block.)\n" +
        "Whenever one or more other creatures you control enter, if this creature is suspected, " +
        "you may suspect one of the other creatures. If you do, this creature is no longer " +
        "suspected."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Suspect(EffectTarget.Self)
        description = "When this creature enters, suspect it."
    }

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Creature, excludeSource = true)
        triggerCondition = Conditions.SourceIsSuspected
        effect = Effects.Composite(
            SelectFromCollectionEffect(
                from = IterationSpace.TRIGGER_CAPTURED_COLLECTION,
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "scapegoated",
                useTargetingUI = true,
                prompt = "You may suspect one of the creatures that entered. " +
                    "If you do, Frantic Scapegoat is no longer suspected."
            ),
            // Inside a ForEach over a collection, EffectTarget.Self rebinds to the iteration
            // item — so this suspects the chosen creature, not the Goat.
            ForEachInCollectionEffect(
                collection = "scapegoated",
                effect = Effects.Suspect(EffectTarget.Self)
            ),
            // Back at the top level, Self is the source again: the Goat sheds its own suspicion,
            // but only if a creature was actually chosen ("If you do").
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("scapegoated"),
                effect = Effects.NoLongerSuspected(EffectTarget.Self)
            ),
        )
        description = "Whenever one or more other creatures you control enter, if this creature " +
            "is suspected, you may suspect one of the other creatures. If you do, this creature " +
            "is no longer suspected."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb81e343-7242-44b1-9ce6-1dddd104f764.jpg?1783912880"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until " +
                "it leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
        ruling(
            "2024-02-02",
            "There's no limit to the number of creatures that can be suspected simultaneously. " +
                "Suspecting a new creature doesn't cause other creatures to stop being suspected."
        )
    }
}
