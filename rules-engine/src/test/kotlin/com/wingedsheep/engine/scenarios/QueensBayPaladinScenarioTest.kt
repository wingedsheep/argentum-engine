package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Queen's Bay Paladin (LCI #115).
 *
 * Queen's Bay Paladin {3}{B}{B}
 * Creature — Vampire Knight 5/4
 * Whenever this creature enters or attacks, return up to one target Vampire card from your
 * graveyard to the battlefield with a finality counter on it. You lose life equal to its mana
 * value. (If a creature with a finality counter on it would die, exile it instead.)
 *
 * The reanimation target is Highborn Vampire ({3}{B}, a vanilla Vampire Warrior, mana value 4),
 * so a successful return costs the controller exactly 4 life.
 *
 * Coverage:
 * 1. Enters trigger — reanimate the chosen Vampire with a finality counter; lose life = its MV.
 * 2. Attacks trigger — same rider fires off the combat trigger, proving the "or attacks" half.
 * 3. "Up to one target" — the controller declines (skips), so nothing returns and no life is lost.
 * 4. "Vampire card" filter — a non-Vampire in the same graveyard is not a legal target and is
 *    left behind while the Vampire is returned.
 */
class QueensBayPaladinScenarioTest : ScenarioTestBase() {

    init {
        test("enters: returns the target Vampire with a finality counter and loses life equal to its mana value") {
            val game = scenario()
                .withPlayers()
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Queen's Bay Paladin")
                .withLandsOnBattlefield(1, "Swamp", 5) // {3}{B}{B}
                .withCardInGraveyard(1, "Highborn Vampire")
                .build()

            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Queen's Bay Paladin").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("the enters trigger paused for a target selection") {
                decision.shouldNotBeNull()
                (decision as ChooseTargetsDecision).legalTargets[0]!! shouldContain
                    game.findCardsInGraveyard(1, "Highborn Vampire").first()
            }

            game.selectTargets(game.findCardsInGraveyard(1, "Highborn Vampire")).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Highborn Vampire")
            withClue("Highborn Vampire returned to the battlefield") {
                returned.shouldNotBeNull()
                game.isInGraveyard(1, "Highborn Vampire") shouldBe false
            }
            withClue("it entered with a finality counter") {
                game.state.getEntity(returned!!)?.get<CountersComponent>()
                    ?.getCount(CounterType.FINALITY) shouldBe 1
            }
            withClue("controller lost life equal to the returned card's mana value (4)") {
                game.getLifeTotal(1) shouldBe lifeBefore - 4
            }
        }

        test("attacks: the same rider fires when Queen's Bay Paladin attacks") {
            val game = scenario()
                .withPlayers()
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .withCardOnBattlefield(1, "Queen's Bay Paladin", summoningSickness = false)
                .withCardInGraveyard(1, "Highborn Vampire")
                .build()

            val lifeBefore = game.getLifeTotal(1)

            game.declareAttackers(mapOf("Queen's Bay Paladin" to 2)).error shouldBe null
            if (game.getPendingDecision() == null) game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("the attacks trigger paused for a target selection") {
                decision.shouldNotBeNull()
            }

            game.selectTargets(game.findCardsInGraveyard(1, "Highborn Vampire")).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Highborn Vampire")
            withClue("Highborn Vampire returned with a finality counter") {
                returned.shouldNotBeNull()
                game.state.getEntity(returned)?.get<CountersComponent>()
                    ?.getCount(CounterType.FINALITY) shouldBe 1
            }
            withClue("controller lost 4 life (Highborn Vampire's mana value)") {
                game.getLifeTotal(1) shouldBe lifeBefore - 4
            }
        }

        test("up to one target: declining returns nothing and loses no life") {
            val game = scenario()
                .withPlayers()
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Queen's Bay Paladin")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInGraveyard(1, "Highborn Vampire")
                .build()

            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Queen's Bay Paladin").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldNotBeNull()
            // Choose no target ("up to one").
            game.skipTargets().error shouldBe null
            game.resolveStack()

            withClue("Highborn Vampire stayed in the graveyard") {
                game.isInGraveyard(1, "Highborn Vampire") shouldBe true
                game.findPermanent("Highborn Vampire") shouldBe null
            }
            withClue("no life was lost because nothing was returned") {
                game.getLifeTotal(1) shouldBe lifeBefore
            }
        }

        test("Vampire filter: a non-Vampire card in the graveyard is not a legal target") {
            val game = scenario()
                .withPlayers()
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Queen's Bay Paladin")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInGraveyard(1, "Highborn Vampire")
                .withCardInGraveyard(1, "Grizzly Bears") // Bear, not a Vampire
                .build()

            game.castSpell(1, "Queen's Bay Paladin").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseTargetsDecision
            val legal = decision.legalTargets[0]!!
            withClue("only the Vampire is a legal target") {
                legal shouldContain game.findCardsInGraveyard(1, "Highborn Vampire").first()
                legal shouldNotContain game.findCardsInGraveyard(1, "Grizzly Bears").first()
            }

            game.selectTargets(game.findCardsInGraveyard(1, "Highborn Vampire")).error shouldBe null
            game.resolveStack()

            withClue("the Vampire was returned; the non-Vampire stayed put") {
                game.findPermanent("Highborn Vampire").shouldNotBeNull()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
