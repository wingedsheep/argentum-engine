package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nissa, Worldwaker (M15 #187, {3}{G}{G}, loyalty 3).
 *
 * - The first +1's animation has no duration (ruling): the land is a 4/4 Elemental with trample
 *   on later turns too, and it stays a land with its land subtype.
 * - The second +1 untaps Forests under any player's control, and only Forests.
 * - The −7 animates exactly the basic lands it put onto the battlefield — not lands already in
 *   play, not lands left in the library — and that animation is permanent too.
 */
class NissaWorldwakerScenarioTest : ScenarioTestBase() {

    private val name = "Nissa, Worldwaker"

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.assertElementalLand(land: EntityId, subtype: String) {
            val projected = state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasType(land, "LAND") shouldBe true
            projected.hasSubtype(land, subtype) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.getPower(land) shouldBe 4
            projected.getToughness(land) shouldBe 4
            projected.hasKeyword(land, Keyword.TRAMPLE) shouldBe true
        }

        test("first +1 makes a land you control a 4/4 Elemental with trample that's still a Forest, permanently") {
            val game = board()
                .withCardOnBattlefield(1, "Forest")
                .build()
            val nissa = game.findPermanent(name)!!
            val forest = game.findPermanent("Forest")!!
            setLoyalty(game, nissa, 3)

            activate(game, nissa, index = 0, targets = listOf(ChosenTarget.Permanent(forest)))
            game.resolveStack()

            game.assertElementalLand(forest, "Forest")
            loyalty(game, nissa) shouldBe 4

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("no \"until end of turn\": still a 4/4 Elemental land on the next turn") {
                game.assertElementalLand(forest, "Forest")
            }
        }

        test("first +1 can't target a land an opponent controls") {
            val game = board()
                .withCardOnBattlefield(2, "Forest")
                .build()
            val nissa = game.findPermanent(name)!!
            val theirForest = game.findPermanent("Forest")!!
            setLoyalty(game, nissa, 3)

            val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[0]
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = nissa,
                    abilityId = ability.id,
                    targets = listOf(ChosenTarget.Permanent(theirForest))
                )
            ).error shouldNotBe null
        }

        test("second +1 untaps Forests of any player, but a Plains isn't a legal target") {
            val game = board()
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withCardOnBattlefield(1, "Plains", tapped = true)
                .build()
            val nissa = game.findPermanent(name)!!
            val forests = game.findAllPermanents("Forest")
            val plains = game.findPermanent("Plains")!!
            setLoyalty(game, nissa, 3)

            val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[1]
            withClue("a Plains isn't a Forest") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = nissa,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(plains))
                    )
                ).error shouldNotBe null
            }

            activate(game, nissa, index = 1, targets = forests.map { ChosenTarget.Permanent(it) })
            game.resolveStack()

            forests.forEach { game.isTapped(it) shouldBe false }
            game.isTapped(plains) shouldBe true
            loyalty(game, nissa) shouldBe 4
        }

        test("−7 puts the chosen basic lands onto the battlefield and only those become 4/4 Elemental tramplers") {
            val game = board()
                .withCardOnBattlefield(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Plains")
                .build()
            val nissa = game.findPermanent(name)!!
            val inPlayForest = game.findPermanent("Forest")!!
            val libraryForests = game.findCardsInLibrary(1, "Forest")
            val chosenForest = libraryForests[0]
            val leftForest = libraryForests[1]
            val plains = game.findCardsInLibrary(1, "Plains").single()
            setLoyalty(game, nissa, 7)

            activate(game, nissa, index = 2)
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only basic lands can be found") {
                decision.options.toSet() shouldBe (libraryForests + plains).toSet()
            }
            game.selectCards(listOf(chosenForest, plains)).error shouldBe null
            game.resolveStack()

            withClue("Nissa paid all 7 loyalty and is gone") { game.findPermanent(name) shouldBe null }
            game.state.getBattlefield() shouldContain chosenForest
            game.state.getBattlefield() shouldContain plains
            game.state.getLibrary(game.player1Id) shouldContain leftForest
            game.state.getLibrary(game.player1Id) shouldNotContain chosenForest
            game.findAllPermanents("Forest") shouldContainExactlyInAnyOrder listOf(inPlayForest, chosenForest)

            game.assertElementalLand(chosenForest, "Forest")
            game.assertElementalLand(plains, "Plains")
            withClue("a land already on the battlefield isn't one of \"those lands\"") {
                game.state.projectedState.isCreature(inPlayForest) shouldBe false
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("no \"until end of turn\": still 4/4 Elemental lands on the next turn") {
                game.assertElementalLand(chosenForest, "Forest")
                game.assertElementalLand(plains, "Plains")
            }
        }

        test("−7 finding nothing animates nothing and Nissa still pays the loyalty") {
            val game = board()
                .withCardOnBattlefield(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .build()
            val nissa = game.findPermanent(name)!!
            val inPlayForest = game.findPermanent("Forest")!!
            val libraryForest = game.findCardsInLibrary(1, "Forest").single()
            setLoyalty(game, nissa, 7)

            activate(game, nissa, index = 2)
            game.resolveStack()
            while (game.getPendingDecision() is SelectCardsDecision) game.skipSelection().error shouldBe null
            game.resolveStack()

            game.findPermanent(name) shouldBe null
            game.state.getLibrary(game.player1Id) shouldContain libraryForest
            game.state.projectedState.isCreature(inPlayForest) shouldBe false
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)?.has<TappedComponent>() == true

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
