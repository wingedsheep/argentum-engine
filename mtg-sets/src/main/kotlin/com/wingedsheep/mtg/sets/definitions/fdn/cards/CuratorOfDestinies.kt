package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Curator of Destinies
 * {4}{U}{U}
 * Creature — Sphinx
 * 5/5
 * This spell can't be countered.
 * Flying
 * When this creature enters, look at the top five cards of your library and separate them into a
 * face-down pile and a face-up pile. An opponent chooses one of those piles. Put that pile into
 * your hand and the other into your graveyard.
 *
 * A "divvy" (CR 700.3) with one concealed pile — the mirror image of Sauron's Ransom, where the
 * *opponent* looks and splits and *you* choose. Here you look and split, and an opponent chooses.
 * Composed entirely from existing pipeline steps; the information asymmetry is masking, not a
 * bespoke decision type:
 *  1. Gather the top five with the default [com.wingedsheep.sdk.scripting.effects.LookAudience.Controller]
 *     — "look at" is a private look for you only, so the opponent never sees the whole five.
 *  2. You separate them: the cards you select become the face-up pile, the rest the face-down pile.
 *     `chooseAnyNumber` allows 0 and 5, matching the ruling that a 5/0 split is legal either way.
 *  3. Re-gather the face-up pile with `revealed = true` so the split becomes public knowledge —
 *     that persisted reveal is what lets the opponent actually see the face-up pile when choosing.
 *     (`reveal` only emits a `CardsRevealedEvent`; it doesn't persist visibility.) The face-down
 *     pile is never revealed, so it renders to the opponent as opaque card backs — they see only
 *     its size.
 *  4. An opponent picks a pile; it goes to your hand (still unrevealed — per the ruling you don't
 *     have to show a face-down pile that ends up in your hand) and the other to your graveyard,
 *     where it becomes public anyway.
 *
 * Edge cases:
 *  - Empty/short library: `ifNotEmpty` skips the whole ability when the gather comes up empty, so
 *     nobody is asked to split or choose between two empty piles. A 1–4 card library still splits.
 *  - "An opponent chooses" is a resolution-time choice, not a target, so it is
 *    [Chooser.Opponent] rather than a `TargetRequirement` (no shroud/targeting restrictions apply).
 *    Per the ruling *you* decide which opponent chooses, which [Chooser.Opponent] handles: with
 *    several opponents the engine first asks you which one picks a pile; with one it is forced.
 */
val CuratorOfDestinies = card("Curator of Destinies") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 5
    toughness = 5
    oracleText = "This spell can't be countered.\n" +
        "Flying\n" +
        "When this creature enters, look at the top five cards of your library and separate them " +
        "into a face-down pile and a face-up pile. An opponent chooses one of those piles. Put " +
        "that pile into your hand and the other into your graveyard."

    cantBeCountered = true
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            // "look at the top five cards of your library"
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(5)), name = "looked")
            ifNotEmpty(looked) {
                // "and separate them into a face-down pile and a face-up pile."
                val piles = chooseAnyNumberSplit(
                    from = looked,
                    chooser = Chooser.Controller,
                    prompt = "Separate the top five cards into a face-up pile and a face-down pile. " +
                        "The cards you select are placed face up; the rest stay face down.",
                    selectedLabel = "Face-up pile",
                    remainderLabel = "Face-down pile",
                    showAllCards = true,
                    alwaysPrompt = true,
                    name = "faceUp",
                    remainderName = "faceDown"
                )
                // The face-up pile is public from here on; the face-down pile stays hidden.
                val faceUp = gather(piles.selected.asSource, revealed = true, name = "faceUpRevealed")
                // "An opponent chooses one of those piles."
                val picked = choosePile(
                    pileA = faceUp,
                    pileB = piles.remainder,
                    pileALabel = "Face-up pile",
                    pileBLabel = "Face-down pile",
                    chooser = Chooser.Opponent,
                    prompt = "Choose a pile. That pile goes to your opponent's hand; the other goes " +
                        "to their graveyard."
                )
                // "Put that pile into your hand and the other into your graveyard."
                toHand(picked.chosen)
                toGraveyard(picked.other)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Ralph Horsley"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9ff79da7-c3f7-4541-87a0-503544c699b5.jpg?1783909120"
        ruling(
            "2024-11-08",
            "A spell or ability that counters spells can still target Curator of Destinies. When " +
                "that spell or ability resolves, Curator of Destinies won't be countered, but any " +
                "additional effects of the countering spell or ability will still happen."
        )
        ruling(
            "2024-11-08",
            "You may split the cards into one pile of five and one pile of zero. The pile of five " +
                "cards could be the face-up pile or the face-down pile. The opponent may choose the " +
                "empty pile to be put into your hand."
        )
        ruling(
            "2024-11-08",
            "You decide which opponent chooses the pile while resolving Curator of Destinies's last ability."
        )
        ruling(
            "2024-11-08",
            "If the opponent chooses the face-down pile to be put into your hand, you don't have to " +
                "reveal the cards in that pile."
        )
    }
}
