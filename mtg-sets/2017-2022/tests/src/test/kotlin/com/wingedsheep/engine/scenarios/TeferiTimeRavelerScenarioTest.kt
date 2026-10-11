package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.FlashGrantsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.war.cards.TeferiTimeRaveler
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Teferi, Time Raveler — the opponent-side sorcery-speed lock (`PlayersCantCastSpells` gated on
 * `Not(CouldCastSorcery)` from the caster's seat), the +1's until-your-next-turn flash grant for
 * sorceries, and the −3's optional bounce plus draw. The lock's CR 307.5 clauses are covered
 * clause-by-clause in `CouldCastSorceryRestrictionTest`.
 */
class TeferiTimeRavelerScenarioTest : ScenarioTestBase() {

    private val plusOne = TeferiTimeRaveler.activatedAbilities[0].id
    private val minusThree = TeferiTimeRaveler.activatedAbilities[1].id

    private fun ScenarioTestBase.TestGame.boltCastable(playerNumber: Int): Boolean {
        val bolt = findCardsInHand(playerNumber, "Lightning Bolt").single()
        return getLegalActions(playerNumber).any { (it.action as? CastSpell)?.cardId == bolt }
    }

    private fun ScenarioTestBase.TestGame.setLoyalty(n: Int) {
        val teferi = findPermanent("Teferi, Time Raveler")!!
        state = state.updateEntity(teferi) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, n)) }
    }

    init {
        context("Teferi, Time Raveler") {

            test("an opponent can't cast an instant on Teferi's controller's turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Time Raveler")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withPriorityPlayer(2)
                    .build()

                game.boltCastable(2) shouldBe false
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
                game.getLifeTotal(1) shouldBe 20
            }

            test("an opponent casts an instant normally in their own main phase with an empty stack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Time Raveler")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.boltCastable(2) shouldBe true
            }

            test("+1 lets you cast a sorcery during an opponent's turn, and the grant lasts until your next turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Time Raveler")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setLoyalty(4)
                val teferi = game.findPermanent("Teferi, Time Raveler")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = teferi, abilityId = plusOne))
                    .error shouldBe null
                game.resolveStack()
                game.state.getEntity(teferi)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("it is now the opponent's end step") { game.state.activePlayerId shouldBe game.player2Id }
                game.state.getEntity(game.player1Id)?.get<FlashGrantsComponent>().shouldNotBeNull()
                while (game.state.priorityPlayerId != game.player1Id) game.passPriority()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 2

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the grant lapsed as Teferi's controller's next turn began") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.getEntity(game.player1Id)?.get<FlashGrantsComponent>().shouldBeNull()
                }
            }

            test("−3 returns the target to its owner's hand and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Time Raveler")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setLoyalty(4)
                val teferi = game.findPermanent("Teferi, Time Raveler")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = teferi, abilityId = minusThree,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 1
                game.state.getEntity(teferi)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 1
            }

            test("−3 with no target still draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Time Raveler")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setLoyalty(4)
                val teferi = game.findPermanent("Teferi, Time Raveler")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = teferi, abilityId = minusThree))
                    .error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
            }
        }
    }
}
