package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fiery Gambit — Mirrodin #90
 * {2}{R} · Sorcery · Rare
 *
 * Flip a coin until you lose a flip or choose to stop flipping. If you lose a flip, Fiery Gambit has
 * no effect. If you win one or more flips, Fiery Gambit deals 3 damage to target creature. If you win
 * two or more flips, Fiery Gambit deals 6 damage to each opponent. If you win three or more flips,
 * draw nine cards and untap all lands you control.
 *
 * Modelling notes:
 * - The whole card is one tally and three thresholds. [Effects.FlipCoinsUntilLoss] runs the open-ended
 *   flip sequence and publishes how many flips were won; each payoff is then a plain
 *   `Compare(wins, GTE, n)` gate over that one number. The tiers are cumulative and *not* exclusive —
 *   three won flips fires all three, which is what the card's ruling spells out.
 * - "If you lose a flip, Fiery Gambit has no effect" needs no branch of its own. A lost first flip
 *   stores 0, an unread pipeline number reads as 0, and every `GTE 1` gate therefore falls away on its
 *   own. Writing the sentence out as a fourth branch would be a second way to express the same thing.
 * - "target creature" is a real, required cast-time target, not "up to one": per the card's ruling an
 *   illegal target on resolution means the spell doesn't resolve and no coin is flipped at all. That is
 *   the engine's default for a single-target spell, so the ordering falls out for free — the flips
 *   happen during resolution, which an unresolved spell never reaches.
 * - Each coin here is its own flip, unlike `Effects.FlipCoins(n)` where the whole batch is one event.
 *   That distinction is what makes a "the first time you flip one or more coins each turn" replacement
 *   (Edgar, King of Figaro) cover only the first coin of a Gambit rather than the entire run.
 * - The stop choice is offered only after a *won* flip; a lost flip has already ended the run, so there
 *   is nothing to ask. That is the "after each flip, you choose whether to continue flipping" ruling.
 */
val FieryGambit = card("Fiery Gambit") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Flip a coin until you lose a flip or choose to stop flipping. If you lose a flip, " +
        "Fiery Gambit has no effect. If you win one or more flips, Fiery Gambit deals 3 damage to " +
        "target creature. If you win two or more flips, Fiery Gambit deals 6 damage to each " +
        "opponent. If you win three or more flips, draw nine cards and untap all lands you control."

    spell {
        val creature = target("target creature", Targets.Creature)

        /** `wins >= threshold` over the tally the flip run published. */
        fun wonAtLeast(threshold: Int) = Gate.WhenCondition(
            Conditions.CompareAmounts(
                DynamicAmount.VariableReference("fieryGambitWins"),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(threshold),
            )
        )

        effect = Effects.FlipCoinsUntilLoss(storeWinsAs = "fieryGambitWins")
            .then(
                GatedEffect(
                    gate = wonAtLeast(1),
                    then = Effects.DealDamage(3, creature),
                    descriptionOverride = "If you win one or more flips, Fiery Gambit deals 3 " +
                        "damage to target creature.",
                )
            )
            .then(
                GatedEffect(
                    gate = wonAtLeast(2),
                    then = Effects.DealDamage(6, EffectTarget.PlayerRef(Player.EachOpponent)),
                    descriptionOverride = "If you win two or more flips, Fiery Gambit deals 6 " +
                        "damage to each opponent.",
                )
            )
            .then(
                GatedEffect(
                    gate = wonAtLeast(3),
                    then = Effects.DrawCards(9)
                        .then(
                            Patterns.Group.untapGroup(
                                GroupFilter(GameObjectFilter.Land.youControl())
                            )
                        ),
                    descriptionOverride = "If you win three or more flips, draw nine cards and " +
                        "untap all lands you control.",
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a91376ed-5868-4887-8389-5ef5b9471786.jpg?1783944541"
        ruling(
            "2004-12-01",
            "You must choose a target creature when you cast Fiery Gambit. If that target isn't " +
                "legal on resolution, Fiery Gambit has no effect and you don't even flip a coin."
        )
        ruling(
            "2004-12-01",
            "You can flip any number of coins (you can even flip more than three), but Fiery Gambit " +
                "has no effect if you lose any of the flips. You can't continue flipping if you " +
                "lose a flip."
        )
        ruling(
            "2004-12-01",
            "If you win three flips, Fiery Gambit deals 3 damage to the target creature and 6 " +
                "damage to each opponent, and you draw nine cards and untap all lands you control."
        )
        ruling("2004-12-01", "After each flip, you choose whether to continue flipping.")
    }
}
