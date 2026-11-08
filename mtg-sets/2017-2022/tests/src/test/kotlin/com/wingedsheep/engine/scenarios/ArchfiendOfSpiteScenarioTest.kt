package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.c19.cards.ArchfiendOfSpite
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Archfiend of Spite — "Whenever a source an opponent controls deals damage to this creature, that
 * source's controller loses that much life unless they sacrifice that many permanents of their
 * choice."
 *
 * The source's controller chooses between the two (never a mix); with fewer than N permanents the
 * sacrifice isn't on offer and the life loss just happens (ruling 2019-08-23).
 */
class ArchfiendOfSpiteScenarioTest : ScenarioTestBase() {

    private val bolt = card("Bolt Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Bolt Test deals 3 damage to target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(3, t)
        }
    }

    /** Opponent (player 2) holds the burn spell and has [mountains] Mountains in play. */
    private fun board(mountains: Int): ScenarioTestBase.ScenarioBuilder {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Archfiend of Spite", summoningSickness = false)
            .withCardOnBattlefield(1, "Swamp")
            .withCardInHand(1, "Bolt Test")
            .withCardInHand(2, "Bolt Test")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(mountains) { builder.withCardOnBattlefield(2, "Mountain") }
        repeat(8) {
            builder.withCardInLibrary(1, "Swamp")
            builder.withCardInLibrary(2, "Mountain")
        }
        return builder
    }

    init {
        cardRegistry.register(ArchfiendOfSpite)
        cardRegistry.register(bolt)

        context("Archfiend of Spite") {

            test("the burn spell's caster may lose that much life instead of sacrificing") {
                val game = board(mountains = 5).build()
                val archfiend = game.findPermanent("Archfiend of Spite")!!

                game.castSpell(2, "Bolt Test", targetId = archfiend).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the spell is gone, yet its controller — not the Archfiend's — is the one asked") {
                    decision.playerId shouldBe game.player2Id
                    decision.options shouldBe listOf("Sacrifice that many permanents", "Lose that much life")
                }
                game.submitDecision(OptionChosenResponse(decision.id, 1)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
                game.findPermanents("Mountain").size shouldBe 5
                game.isOnBattlefield("Archfiend of Spite") shouldBe true
            }

            test("or sacrifice exactly that many permanents of their choice, losing no life") {
                val game = board(mountains = 5).build()
                val archfiend = game.findPermanent("Archfiend of Spite")!!
                val mountains = game.findPermanents("Mountain")

                game.castSpell(2, "Bolt Test", targetId = archfiend).error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(choice.id, 0)).error shouldBe null

                val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster picks 3 of their own 5 permanents") {
                    pick.playerId shouldBe game.player2Id
                    pick.minSelections shouldBe 3
                    pick.maxSelections shouldBe 3
                    pick.options.toSet() shouldBe mountains.toSet()
                }
                game.selectCards(mountains.take(3)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
                game.findPermanents("Mountain") shouldBe mountains.drop(3)
            }

            test("with fewer permanents than the damage, they must lose the life — no choice is offered") {
                val game = board(mountains = 2).build()
                val archfiend = game.findPermanent("Archfiend of Spite")!!

                game.castSpell(2, "Bolt Test", targetId = archfiend).error shouldBe null
                game.resolveStack()

                withClue("two permanents can't pay for 3 damage: only the life loss remains, so nothing is asked") {
                    game.hasPendingDecision() shouldBe false
                    game.getLifeTotal(2) shouldBe 17
                    game.findPermanents("Mountain").size shouldBe 2
                }
            }

            test("damage from its own controller's source doesn't trigger it") {
                val game = board(mountains = 5).withActivePlayer(1).build()
                val archfiend = game.findPermanent("Archfiend of Spite")!!

                game.castSpell(1, "Bolt Test", targetId = archfiend).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
