package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
