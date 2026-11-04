package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class JaceCunningCastawayScenarioTest : ScenarioTestBase() {
    private val name = "Jace, Cunning Castaway"

    private fun loyaltyOf(game: TestGame, entity: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(entity)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun activate(game: TestGame, index: Int, loyalty: Int) {
        val source = game.findPermanent(name)!!
        game.state = game.state.updateEntity(source) {
            it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty))
        }
        game.execute(
            ActivateAbility(game.player1Id, source, cardRegistry.getCard(name)!!.script.activatedAbilities[index].id)
        ).error shouldBe null
        game.resolveStack()
    }

    init {
        test("+1: after Jace leaves, two creatures connecting in one damage step loot exactly once") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Boomerang")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            activate(game, 0, loyalty = 3)
            loyaltyOf(game, game.findPermanent(name)!!) shouldBe 4
            withClue("nothing happens until creatures deal combat damage") {
                game.handSize(1) shouldBe 2
            }
            game.castSpell(1, "Boomerang", game.findPermanent(name)!!).error shouldBe null
            game.resolveStack()
            game.findPermanent(name) shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 15
            val discard = game.getPendingDecision() as? SelectCardsDecision
                ?: error("expected a discard selection; got ${game.getPendingDecision()}")
            discard.options.size shouldBe 3
            game.selectCards(game.findCardsInHand(1, "Shock")).error shouldBe null
            game.resolveStack()

            withClue("one batched trigger for both creatures: one draw, one discard") {
                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Shock") shouldBe true
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 1
            }
        }

        test("+1: double strike loots once in each combat damage step") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Fencing Ace", summoningSickness = false)
                .withCardInHand(1, "Shock")
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            activate(game, 0, loyalty = 3)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Fencing Ace" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null

            for ((index, step) in listOf(Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE).withIndex()) {
                game.passUntilPhase(Phase.COMBAT, step)
                game.getLifeTotal(2) shouldBe 19 - index
                game.resolveStack()
                val discard = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected loot during $step; got ${game.getPendingDecision()}")
                discard.options.size shouldBe 2
                game.selectCards(listOf(discard.options.first())).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 2 - index
            }
        }

        test("+1: simultaneous damage to two players loots once per damaged player") {
            val driver = GameTestDriver()
            val jace = cardRegistry.getCard(name)!!
            driver.registerCards(TestCards.all + listOf(jace))
            val (controller, opponent1, opponent2) = driver.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val source = driver.putPermanentOnBattlefield(controller, name)
            driver.replaceState(driver.state.updateEntity(source) {
                it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
            })
            val attackers = List(3) {
                driver.putCreatureOnBattlefield(controller, "Grizzly Bears").also {
                    driver.removeSummoningSickness(it)
                }
            }
            driver.submit(ActivateAbility(controller, source, jace.script.activatedAbilities[0].id)).error shouldBe null
            while (driver.stackSize > 0) driver.bothPass().error shouldBe null
            val initialHandSize = driver.getHandSize(controller)
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(controller, mapOf(
                attackers[0] to opponent1, attackers[1] to opponent1, attackers[2] to opponent2,
            )).error shouldBe null
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareBlockers(opponent1, emptyMap()).error shouldBe null
            driver.declareBlockers(opponent2, emptyMap()).error shouldBe null
            driver.passPriorityUntil(Step.COMBAT_DAMAGE)
            driver.getLifeTotal(opponent1) shouldBe 16
            driver.getLifeTotal(opponent2) shouldBe 18

            var loots = 0
            var passes = 0
            while (driver.stackSize > 0 || driver.pendingDecision != null) {
                check(passes++ < 30) { "combat triggers failed to finish" }
                val decision = driver.pendingDecision
                when {
                    decision is SelectCardsDecision -> {
                        decision.playerId shouldBe controller
                        driver.getHandSize(controller) shouldBe initialHandSize + 1
                        driver.submitCardSelection(controller, listOf(decision.options.first())).error shouldBe null
                        loots++
                    }
                    decision is OrderTriggersDecision -> driver.submitDecision(
                        decision.playerId, TriggerOrderResponse(decision.id, decision.triggers.indices.toList()),
                    ).error shouldBe null
                    decision != null -> error("unexpected decision: $decision")
                    else -> driver.bothPass().error shouldBe null
                }
            }
            withClue("two recipients mean two loots, even though three creatures connected") {
                loots shouldBe 2
                driver.getHandSize(controller) shouldBe initialHandSize
            }
        }

        test("−2: the Illusion ignores ability targeting but is sacrificed when targeted by a spell") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Prodigal Pyromancer", summoningSickness = false)
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            activate(game, 1, loyalty = 3)
            loyaltyOf(game, game.findPermanent(name)!!) shouldBe 1
            val illusion = game.state.getBattlefield(game.player1Id).single {
                game.state.projectedState.hasSubtype(it, "Illusion")
            }
            game.state.projectedState.getPower(illusion) shouldBe 2
            game.state.projectedState.getToughness(illusion) shouldBe 2

            val pyromancer = game.findPermanent("Prodigal Pyromancer")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, pyromancer,
                    cardRegistry.getCard("Prodigal Pyromancer")!!.script.activatedAbilities.single().id,
                    listOf(ChosenTarget.Permanent(illusion)),
                )
            ).error shouldBe null
            game.resolveStack()
            withClue("targeting by an ability must not sacrifice the Illusion") {
                (illusion in game.state.getBattlefield(game.player1Id)) shouldBe true
            }

            game.castSpell(1, "Shock", illusion).error shouldBe null
            game.resolveStack()

            withClue("the sacrifice trigger resolves first; Shock then has no legal target") {
                (illusion in game.state.getBattlefield(game.player1Id)) shouldBe false
                game.isInGraveyard(1, "Shock") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("−5: two non-legendary token copies enter with three loyalty each after Jace dies paying the cost") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            activate(game, 2, loyalty = 5)

            game.isInGraveyard(1, name) shouldBe true
            val copies = game.findAllPermanents(name)
            copies.size shouldBe 2
            for (copy in copies) {
                game.state.getEntity(copy)!!.has<TokenComponent>() shouldBe true
                game.state.projectedState.isLegendary(copy) shouldBe false
                loyaltyOf(game, copy) shouldBe 3
            }
        }
    }
}
