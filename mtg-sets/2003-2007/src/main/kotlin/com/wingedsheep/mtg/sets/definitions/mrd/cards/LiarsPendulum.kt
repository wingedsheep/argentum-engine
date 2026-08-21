package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Liar's Pendulum — Mirrodin #196
 * {1} · Artifact · Rare
 *
 * {2}, {T}: Choose a card name. Target opponent guesses whether a card with that name is in your
 * hand. You may reveal your hand. If you do and your opponent guessed wrong, draw a card.
 *
 * Modelling notes:
 * - Four steps in printed order: you name a card, the opponent guesses, you *may* reveal, and the
 *   draw hangs off both the reveal and the guess being wrong. The card's rulings are explicit that the
 *   reveal is offered "regardless of whether your opponent guessed right or wrong" and that you
 *   "can't draw a card if you don't reveal your hand" — so the reveal sits outside the right/wrong
 *   split, and the draw sits *inside* the reveal.
 * - That middle step is why the guess stores a number instead of branching.
 *   [Effects.PlayerGuessesCondition] scores the guess into `pendulumGuessedRight` and stops; a
 *   guess primitive that carried "on right" / "on wrong" effects would have to hold the optional
 *   reveal in both branches, which both duplicates it and makes the two prompts tell the guesser
 *   whether they were right.
 * - The proposition is an ordinary resolution-time condition over the controller's hand, matched by
 *   the name chosen a step earlier (`namedFromVariable`). It is evaluated only after the answer is in
 *   and never shown to the guesser, so the bluff is real: nothing about your hand is public unless you
 *   choose to reveal it.
 * - The guesser is `Chooser.TargetPlayer` — the printed "target opponent" — so the question goes to the
 *   opponent this activation targeted, not to the artifact's controller.
 * - "Guessed wrong" is `pendulumGuessedRight == 0`. Reading the variable rather than a branch keeps the
 *   whole payoff in one place, and it is only read after the guess has written it.
 */
val LiarsPendulum = card("Liar's Pendulum") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Choose a card name. Target opponent guesses whether a card with that " +
        "name is in your hand. You may reveal your hand. If you do and your opponent guessed " +
        "wrong, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        target("target opponent", Targets.Opponent)

        effect = Effects.ChooseCardName(
            storeAs = "pendulumName",
            prompt = "Choose a card name"
        )
            .then(
                Effects.PlayerGuessesCondition(
                    condition = Exists(
                        player = Player.You,
                        zone = Zone.HAND,
                        filter = GameObjectFilter.Any.namedFromVariable("pendulumName")
                    ),
                    // "your opponent" reads from the guesser's side; the decision also carries this
                    // artifact's name, so a multiplayer table can still tell whose hand is meant.
                    prompt = "Is a card named \"{name}\" in your opponent's hand?",
                    storeGuessedRightAs = "pendulumGuessedRight",
                    guesser = Chooser.TargetPlayer,
                    promptNameVariable = "pendulumName"
                )
            )
            .then(
                MayEffect(
                    effect = RevealHandEffect(EffectTarget.Controller)
                        .then(
                            GatedEffect(
                                gate = Gate.WhenCondition(
                                    Conditions.CompareAmounts(
                                        DynamicAmount.VariableReference("pendulumGuessedRight"),
                                        ComparisonOperator.EQ,
                                        DynamicAmount.Fixed(0),
                                    )
                                ),
                                then = Effects.DrawCards(1),
                                descriptionOverride = "If your opponent guessed wrong, draw a card.",
                            )
                        ),
                    descriptionOverride = "reveal your hand",
                )
            )

        description = "Choose a card name. Target opponent guesses whether a card with that name " +
            "is in your hand. You may reveal your hand. If you do and your opponent guessed " +
            "wrong, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66372597-c182-4850-a630-231737b1482b.jpg?1783944515"
        ruling(
            "2004-12-01",
            "First you name a card. Then the targeted opponent guesses whether the card is in your hand."
        )
        ruling(
            "2004-12-01",
            "After the opponent has guessed, you may reveal your hand. If your opponent was wrong, " +
                "you draw a card. If your opponent was right, you don't draw a card."
        )
        ruling(
            "2004-12-01",
            "You reveal your hand only if you choose to, regardless of whether your opponent " +
                "guessed right or wrong."
        )
        ruling("2004-12-01", "You can't draw a card if you don't reveal your hand.")
    }
}
