package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.EvidenceCollectedEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Incinerator of the Guilty (MKM) — "Flying, trample. Whenever this creature deals combat damage to
 * a player, you may collect evidence X. When you do, this creature deals X damage to each creature
 * and planeswalker that player controls."
 *
 * The new vocabulary is `CollectEvidenceChosenAmountEffect`, and what these tests pin is its two
 * hops in the right order:
 *
 *  1. **X is chosen, bounded by the graveyard's total mana value** — CR 701.59b applied to the
 *     choice, so an unreachable X is never offered.
 *  2. **Then the cards** — and X, not the exiled total, is what the payoff spends. Over-exiling is
 *     legal (CR 701.59a), so deriving X from the exiled cards would silently inflate the damage.
 *
 * X = 0 is the interesting edge: per the 2024-02-02 ruling it is a legal collection that exiles
 * nothing and *still counts* as collecting evidence, so `EvidenceCollectedEvent` must fire for
 * "whenever you collect evidence" payoffs. That is also why the "may" is offered no matter how thin
 * the graveyard — unlike the fixed-N `Effects.CollectEvidence`, which is suppressed when it can't
 * be paid.
 */
class IncineratorOfTheGuiltyScenarioTest : ScenarioTestBase() {

    init {
        test("collecting evidence 3 deals 3 to each creature that player controls") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Incinerator of the Guilty", summoningSickness = false)
                // Graveyard totalling 6 mana value, so X may be anything from 0 to 6.
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Air Elemental")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Incinerator of the Guilty" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            // "You may collect evidence X" — yes.
            game.answerYesNo(true)
            // Choose X = 3.
            game.chooseNumber(3)
            // Then pick the cards to exile: Centaur Courser alone is mana value 3.
            game.selectCards(listOfNotNull(game.findCardsInGraveyard(1, "Centaur Courser").firstOrNull()))
            game.resolveStack()

            withClue("Centaur Courser paid the 3, Air Elemental stayed put") {
                game.isInExile(1, "Centaur Courser") shouldBe true
                game.isInGraveyard(1, "Air Elemental") shouldBe true
            }
            withClue("3 damage kills both — a 2/2 and a 1/1") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            }
        }

        test("X is what was chosen, not the total exiled — over-exiling does not inflate the damage") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Incinerator of the Guilty", summoningSickness = false)
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Air Elemental")
                // A 3/3 survives 1 damage but not 3.
                .withCardOnBattlefield(2, "Gray Ogre")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Incinerator of the Guilty" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.answerYesNo(true)
            // Choose X = 1, then deliberately exile far more than needed.
            game.chooseNumber(1)
            game.selectCards(
                listOfNotNull(
                    game.findCardsInGraveyard(1, "Centaur Courser").firstOrNull(),
                    game.findCardsInGraveyard(1, "Air Elemental").firstOrNull(),
                )
            )
            game.resolveStack()

            withClue("both cards left the graveyard — over-exiling is the player's right") {
                game.isInExile(1, "Centaur Courser") shouldBe true
                game.isInExile(1, "Air Elemental") shouldBe true
            }
            withClue("but the damage is the chosen X of 1, not the exiled total of 6") {
                game.isOnBattlefield("Gray Ogre") shouldBe true
            }
        }

        test("X = 0 exiles nothing and still counts as collecting evidence") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Incinerator of the Guilty", summoningSickness = false)
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Incinerator of the Guilty" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.answerYesNo(true)
            val results = game.chooseNumber(0)
            game.resolveStack()

            withClue("nothing was exiled") {
                game.isInGraveyard(1, "Centaur Courser") shouldBe true
            }
            withClue("0 damage killed nothing") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("but it still counts — 'whenever you collect evidence' must see it") {
                results.events.any { it is EvidenceCollectedEvent } shouldBe true
            }
        }

        test("an empty graveyard still offers the may — X can only be 0") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Incinerator of the Guilty", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Incinerator of the Guilty" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            withClue("unlike fixed-N collect evidence, the option is never suppressed") {
                (game.state.pendingDecision != null) shouldBe true
            }
            game.answerYesNo(true)
            game.resolveStack()

            withClue("no prompt for X — 0 is the only legal value — and nothing dies") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("declining the collection deals no damage") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Incinerator of the Guilty", summoningSickness = false)
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Incinerator of the Guilty" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
