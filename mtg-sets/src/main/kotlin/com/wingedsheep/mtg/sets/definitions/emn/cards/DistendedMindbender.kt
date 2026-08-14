package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Distended Mindbender
 * {8}
 * Creature — Eldrazi Insect
 * 5/5
 *
 * Emerge {5}{B}{B}
 * When you cast this spell, target opponent reveals their hand. You choose from it a nonland card
 * with mana value 3 or less and a card with mana value 4 or greater. That player discards those
 * cards.
 *
 * Implementation notes:
 * - Emerge is the engine keyword (CR 702.119) via the `emerge(cost)` helper.
 * - Two selections from one revealed hand, each with its own filter, so it's a gather + two
 *   `chooseExactly` steps rather than a single restricted selection. The two mana-value bands are
 *   disjoint, so no card can be picked twice; a hand with nothing in a band simply yields no pick
 *   for that band (you choose "up to" what exists — CR 608.2 does as much as possible).
 * - `MoveType.Discard` routes both picks through the discard path so discard triggers and madness
 *   (CR 702.35a) apply. They go as two moves rather than one simultaneous discard — the only
 *   observable difference would be an effect that cares about simultaneity, of which there is none
 *   in this engine.
 */
val DistendedMindbender = card("Distended Mindbender") {
    manaCost = "{8}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Insect"
    power = 5
    toughness = 5
    oracleText = "Emerge {5}{B}{B} (You may cast this spell by sacrificing a creature and paying " +
        "the emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, target opponent reveals their hand. You choose from it a " +
        "nonland card with mana value 3 or less and a card with mana value 4 or greater. That " +
        "player discards those cards."

    emerge("{5}{B}{B}")

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Pipeline {
            run(RevealHandEffect(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "hand")
            val cheap = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter.Nonland.manaValueAtMost(3),
                prompt = "Choose a nonland card with mana value 3 or less",
                alwaysPrompt = true,
                showAllCards = true,
                name = "cheap",
            )
            val expensive = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter.Any.manaValueAtLeast(4),
                prompt = "Choose a card with mana value 4 or greater",
                alwaysPrompt = true,
                showAllCards = true,
                name = "expensive",
            )
            move(
                cheap,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard,
            )
            move(
                expensive,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard,
            )
        }
        description = "When you cast this spell, target opponent reveals their hand. You choose " +
            "from it a nonland card with mana value 3 or less and a card with mana value 4 or " +
            "greater. That player discards those cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Yohann Schepacz"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b5d1e41-fb0b-4866-912a-2a7d49542428.jpg?1783937528"
    }
}
