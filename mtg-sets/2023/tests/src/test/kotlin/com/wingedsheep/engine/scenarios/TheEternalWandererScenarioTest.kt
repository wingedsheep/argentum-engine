package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.TheEternalWanderer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Eternal Wanderer (ONE #11).
 *
 * Static: at most one creature may attack the Wanderer each combat. +1 exiles up to one artifact or
 * creature and returns it under its owner's control at *that player's* next end step. 0 makes a
 * 2/2 white Samurai with double strike. −4: the Wanderer's controller picks one creature per player;
 * every other creature is sacrificed.
 */
class TheEternalWandererScenarioTest : ScenarioTestBase() {

    private val exileAbility = TheEternalWanderer.activatedAbilities[0].id
    private val samuraiAbility = TheEternalWanderer.activatedAbilities[1].id
    private val wrathAbility = TheEternalWanderer.activatedAbilities[2].id

    init {
        context("The Eternal Wanderer") {

            test("no more than one creature can attack The Eternal Wanderer each combat") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(2, "The Eternal Wanderer")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val wanderer = game.findPermanent("The Eternal Wanderer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!

                withClue("two creatures on the Wanderer is illegal") {
                    game.execute(
                        DeclareAttackers(game.player1Id, mapOf(bears to wanderer, lions to wanderer))
                    ).error shouldNotBe null
                }
                withClue("one on the Wanderer and one on its controller is fine") {
                    game.execute(
                        DeclareAttackers(game.player1Id, mapOf(bears to wanderer, lions to game.player2Id))
                    ).error shouldBe null
                }
            }

            test("+1 on an opponent's creature returns it at that player's next end step, not yours") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Eternal Wanderer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val wanderer = game.findPermanent("The Eternal Wanderer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, wanderer, exileAbility, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()
                game.isInExile(2, "Grizzly Bears") shouldBe true
                loyalty(game, wanderer) shouldBe 6

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("Wanderer's controller's end step is not the owner's end step") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("returns under its owner's control at the owner's end step") {
                    val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                    game.state.projectedState.getController(returned) shouldBe game.player2Id
                }
            }

            test("+1 on your own permanent returns it at this turn's end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Eternal Wanderer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val wanderer = game.findPermanent("The Eternal Wanderer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, wanderer, exileAbility, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()
                game.isInExile(1, "Grizzly Bears") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.state.activePlayerId shouldBe game.player1Id
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
            }

            test("+1 with no target still adds loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Eternal Wanderer")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val wanderer = game.findPermanent("The Eternal Wanderer")!!

                game.execute(ActivateAbility(game.player1Id, wanderer, exileAbility)).error shouldBe null
                game.resolveStack()
                loyalty(game, wanderer) shouldBe 6
            }

            test("0: creates a 2/2 white Samurai with double strike") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Eternal Wanderer")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val wanderer = game.findPermanent("The Eternal Wanderer")!!

                game.execute(ActivateAbility(game.player1Id, wanderer, samuraiAbility)).error shouldBe null
                game.resolveStack()

                val token = game.findPermanent("Samurai Token").shouldNotBeNull()
                val projected = game.state.projectedState
                projected.getPower(token) shouldBe 2
                projected.getToughness(token) shouldBe 2
                projected.hasKeyword(token, Keyword.DOUBLE_STRIKE) shouldBe true
                projected.hasSubtype(token, "Samurai") shouldBe true
                projected.getColors(token) shouldBe setOf(Color.WHITE.name)
                loyalty(game, wanderer) shouldBe 5
            }

            test("−4: you keep one creature per player; every other creature is sacrificed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Eternal Wanderer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val wanderer = game.findPermanent("The Eternal Wanderer")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val goblin = game.findPermanent("Raging Goblin")!!

                game.execute(ActivateAbility(game.player1Id, wanderer, wrathAbility)).error shouldBe null
                game.resolveStack()

                val picks = mutableListOf<EntityId>()
                while (game.hasPendingDecision()) {
                    withClue("the Wanderer's controller makes every choice") {
                        game.getPendingDecision()!!.playerId shouldBe game.player1Id
                    }
                    val pick = if (picks.isEmpty()) lions else goblin
                    picks += pick
                    game.selectCards(listOf(pick)).error shouldBe null
                }
                game.resolveStack()

                picks.size shouldBe 2
                game.findPermanent("Savannah Lions") shouldBe lions
                game.findPermanent("Raging Goblin") shouldBe goblin
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                val survivors = game.state.getBattlefield().filter { game.state.projectedState.isCreature(it) }
                survivors shouldContainExactlyInAnyOrder listOf(lions, goblin)
                loyalty(game, wanderer) shouldBe 1
            }
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

}
