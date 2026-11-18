package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra, Pyromaster (M14 #132, {2}{R}{R}, Loyalty 4).
 *
 *   +1: Chandra deals 1 damage to target player or planeswalker and 1 damage to up to one target
 *       creature that player or that planeswalker's controller controls. That creature can't block
 *       this turn.
 *   −7: Exile the top ten cards of your library. Choose an instant or sorcery card exiled this way
 *       and copy it three times. You may cast the copies without paying their mana costs.
 *
 * The +1 pins the dependent second target, its "up to one", and the can't-block rider on that
 * creature only. The −7 pins the exile-ten → choose → three-copies → three-free-casts chain: each
 * copy is its own "may", the card itself stays exiled, and only ten cards leave the library.
 */
class ChandraPyromasterScenarioTest : ScenarioTestBase() {

    init {
        context("the +1") {

            test("damages the player and their creature, and that creature can't block this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Pyromaster")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Pyromaster")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                activate(
                    game, chandra, index = 0,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(bears)),
                ).error shouldBe null
                game.resolveStack()

                withClue("1 to the opponent, 1 to the Bears, Chandra up to 5") {
                    game.getLifeTotal(2) shouldBe 19
                    game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
                    loyalty(game, chandra) shouldBe 5
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the damaged Bears can't block") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
                }
                withClue("an untargeted creature still can") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Hill Giant"))).error shouldBe null
                }
            }

            test("the creature target is optional") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Pyromaster")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Pyromaster")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                activate(game, chandra, index = 0, targets = listOf(ChosenTarget.Player(game.player2Id)))
                    .error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.state.getEntity(bears)?.get<DamageComponent>() shouldBe null
            }

            test("the creature must be controlled by the targeted player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Pyromaster")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Pyromaster")!!
                val giant = game.findPermanent("Hill Giant")!!
                activate(
                    game, chandra, index = 0,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(giant)),
                ).error shouldNotBe null
            }
        }

        context("the −7") {

            test("exiles ten, and all three copies of the chosen instant may be cast for free") {
                val game = ultimateBoard()
                val chandra = game.findPermanent("Chandra, Pyromaster")!!
                setLoyalty(game, chandra, 7)
                activate(game, chandra, index = 2).error shouldBe null
                drive(game, castAnswers = listOf(true, true, true))

                withClue("three Bolt copies: 9 damage") { game.getLifeTotal(2) shouldBe 11 }
                withClue("only the top ten left the library; the card itself stays exiled") {
                    game.librarySize(1) shouldBe 2
                    game.state.getExile(game.player1Id).size shouldBe 10
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                }
                withClue("−7 from 7 loyalty: Chandra is gone") {
                    game.findPermanent("Chandra, Pyromaster") shouldBe null
                }
            }

            test("each copy is its own may — casting one and declining two deals 3") {
                val game = ultimateBoard()
                val chandra = game.findPermanent("Chandra, Pyromaster")!!
                setLoyalty(game, chandra, 7)
                activate(game, chandra, index = 2).error shouldBe null
                drive(game, castAnswers = listOf(true, false, false))

                game.getLifeTotal(2) shouldBe 17
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }
        }
    }

    /** Chandra in play over a 12-card library with one Lightning Bolt inside the top ten. */
    private fun ultimateBoard(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Chandra, Pyromaster")
        repeat(4) { builder.withCardInLibrary(1, "Mountain") }
        builder.withCardInLibrary(1, "Lightning Bolt")
        repeat(7) { builder.withCardInLibrary(1, "Mountain") }
        return builder
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Resolve the −7, choosing the Bolt, answering each cast offer in turn, aiming at the opponent. */
    private fun drive(game: TestGame, castAnswers: List<Boolean>) {
        val answers = castAnswers.iterator()
        var guard = 0
        while (guard++ < 40) {
            when (val pending = game.getPendingDecision()) {
                is SelectCardsDecision -> game.selectCards(
                    pending.options.filter {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                    }.take(1)
                )
                is YesNoDecision -> game.answerYesNo(answers.next())
                is ChooseTargetsDecision -> game.selectTargets(listOf(game.player2Id))
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                else -> error("Unexpected decision: $pending")
            }
        }
        withClue("every cast offer was asked") { answers.hasNext() shouldBe false }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ): ExecutionResult {
        val ability = cardRegistry.getCard("Chandra, Pyromaster")!!.script.activatedAbilities[index]
        return game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        )
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.LOYALTY to amount)))
        }
    }
}
