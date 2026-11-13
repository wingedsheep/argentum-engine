package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kiora, Master of the Depths (BFZ #213, {2}{G}{U}, Loyalty 4).
 *
 *   +1: Untap up to one target creature and up to one target land.
 *   −2: Reveal the top four cards of your library. You may put a creature card and/or a land card
 *       from among them into your hand. Put the rest into your graveyard.
 *   −8: You get an emblem with "Whenever a creature you control enters, you may have it fight
 *       target creature." Then create three 8/8 blue Octopus creature tokens.
 *
 * The −8 is the interaction worth pinning: the emblem exists before the Octopuses are created, so
 * all three trigger it in the same resolution (2015-08-25 ruling).
 */
class KioraMasterOfTheDepthsScenarioTest : ScenarioTestBase() {

    init {
        test("+1 untaps a target creature and a target land") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, Master of the Depths")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, Master of the Depths")!!
            setLoyalty(game, kiora, 4)
            val bears = game.findPermanent("Grizzly Bears")!!
            val forest = game.findPermanent("Forest")!!

            activate(game, kiora, index = 0, targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(forest)))
            game.resolveStack()

            isTapped(game, bears) shouldBe false
            isTapped(game, forest) shouldBe false
            loyalty(game, kiora) shouldBe 5
        }

        test("+1 with no targets still adds a loyalty counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, Master of the Depths")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, Master of the Depths")!!
            setLoyalty(game, kiora, 4)

            activate(game, kiora, index = 0, targets = emptyList())
            game.resolveStack()

            loyalty(game, kiora) shouldBe 5
        }

        test("−2 puts one creature and one land into hand and the rest into the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, Master of the Depths")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, Master of the Depths")!!
            setLoyalty(game, kiora, 4)
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val forest = game.findCardsInLibrary(1, "Forest").single()

            activate(game, kiora, index = 1, targets = emptyList())
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bears)).error shouldBe null
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(forest)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
            withClue("the unchosen land and the non-creature, non-land card go to the graveyard") {
                game.isInGraveyard(1, "Island") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }
            loyalty(game, kiora) shouldBe 2
        }

        test("−8 emblem triggers for each Octopus it creates, and each may fight") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, Master of the Depths")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, Master of the Depths")!!
            setLoyalty(game, kiora, 8)
            val victims = ArrayDeque(
                listOf(
                    game.findPermanent("Grizzly Bears")!!,
                    game.findPermanent("Hill Giant")!!,
                    game.findPermanent("Craw Wurm")!!,
                )
            )

            activate(game, kiora, index = 2, targets = emptyList())
            var targetPrompts = 0
            var guard = 0
            while (guard++ < 50) {
                when (val d = game.state.pendingDecision) {
                    is ChooseTargetsDecision -> {
                        targetPrompts++
                        game.selectTargets(listOf(victims.removeFirst())).error shouldBe null
                    }
                    is YesNoDecision -> game.answerYesNo(true).error shouldBe null
                    is OrderObjectsDecision ->
                        game.execute(SubmitDecision(d.playerId, OrderedResponse(d.id, d.objects))).error shouldBe null
                    is OrderTriggersDecision ->
                        game.execute(
                            SubmitDecision(d.playerId, TriggerOrderResponse(d.id, d.triggers.indices.toList()))
                        ).error shouldBe null
                    null -> if (game.state.stack.isEmpty()) break else game.resolveStack()
                    else -> error("unexpected decision ${d::class.simpleName}")
                }
            }

            withClue("Kiora died at 0 loyalty; the emblem is global") {
                game.findPermanent("Kiora, Master of the Depths") shouldBe null
                game.state.globalGrantedTriggeredAbilities.size shouldBe 1
            }
            game.findPermanents("Octopus Token").size shouldBe 3
            withClue("all three Octopuses triggered the emblem") { targetPrompts shouldBe 3 }
            withClue("each 8/8 Octopus fought and killed an opposing creature") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget>) {
        val ability = cardRegistry.getCard("Kiora, Master of the Depths")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
