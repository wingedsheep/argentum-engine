package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rowan's Grim Search
 * {2}{B}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * If this spell was bargained, look at the top four cards of your library, then put up to two of
 * them back on top of your library in any order and the rest into your graveyard.
 * You draw two cards and you lose 2 life.
 *
 * The spell-rider shape of bargain (CR 702.166c) like [CandyGrapple]: the bargained fact is read
 * off the spell while it's still on the stack, so the dig is a [ConditionalEffect] gated on
 * [Conditions.WasBargained] wrapping the whole clause — an unbargained cast is just "draw two,
 * lose 2".
 *
 * The dig itself is a surveil-shaped pipeline with the selection inverted: the *kept* cards are
 * the ones that go back on top ("up to two", so [chooseUpToSplit] rather than a fixed count) and
 * the remainder falls into the graveyard. "In any order" is `CardOrder.ControllerChooses`, which
 * is what `toLibraryTop` already defaults to. `ChooseUpTo` caps at the collection size, so a
 * library of fewer than four cards simply offers what's there rather than deadlocking, and an
 * empty library skips the choice entirely.
 *
 * `Patterns.Library.lookAtTopAndKeep` is the same gather → split → keep/rest shape, but it hard-codes
 * `SelectionMode.ChooseExactly` and this card needs "up to two", so the pipeline is inlined here
 * rather than the pattern gaining a selection-mode parameter for a single caller. A second
 * "look at top N, keep up to M" card is the moment to widen the pattern instead of copying this.
 *
 * The draw and the life loss are unconditional and come *after* the dig, matching the printed
 * order — a bargained cast can therefore bin two cards and then draw into a freshly stacked top.
 */
val RowansGrimSearch = card("Rowan's Grim Search") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "If this spell was bargained, look at the top four cards of your library, then put up to " +
        "two of them back on top of your library in any order and the rest into your graveyard.\n" +
        "You draw two cards and you lose 2 life."

    bargain()

    spell {
        effect = Effects.Composite(
            ConditionalEffect(
                condition = Conditions.WasBargained,
                effect = Effects.Pipeline {
                    val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)))
                    val (kept, rest) = chooseUpToSplit(
                        2,
                        from = looked,
                        prompt = "Put up to two cards back on top of your library",
                        selectedLabel = "Put on top",
                        remainderLabel = "Put in graveyard",
                    )
                    toLibraryTop(kept)
                    toGraveyard(rest)
                },
            ),
            Effects.DrawCards(2),
            Effects.LoseLife(2, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1be6786e-0569-42dd-b03c-82da7b32a14f.jpg?1783915104"

        ruling(
            "2023-09-01",
            "Bargain represents an optional additional cost. A spell cast with that additional " +
                "cost paid is \"bargained.\""
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "If you copy a bargained spell, the copy is also bargained. If a card or token enters " +
                "the battlefield as a copy of a permanent that's already on the battlefield, the " +
                "new permanent isn't bargained, even if the original was."
        )
    }
}
