package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Realm Seekers — {4}{G}{G} 0/0 that enters with a +1/+1 counter for each card in every player's
 * hand, and spends a counter (plus {2}{G}) to tutor a land to hand.
 *
 * Pinned: the entry count reads both players' hands as it enters (the cast Seekers is on the
 * stack, so it doesn't count itself), and the activation's counter cost comes off the Seekers.
 */
class RealmSeekersScenarioTest : ScenarioTestBase() {

    init {
        context("Realm Seekers") {

            fun plusOneCounters(game: TestGame): Int =
                game.findPermanent("Realm Seekers")?.let { id ->
                    game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
                } ?: 0

            fun castSeekers(forests: Int): TestGame {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Realm Seekers")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", forests)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Realm Seekers")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                return game
            }

            test("it enters with a counter for each card in all players' hands, not counting itself") {
                val game = castSeekers(forests = 6)

                withClue("two cards left in the caster's hand plus three in the opponent's") {
                    plusOneCounters(game) shouldBe 5
                }
                val seekers = game.findPermanent("Realm Seekers")!!
                game.state.projectedState.getPower(seekers) shouldBe 5
                game.state.projectedState.getToughness(seekers) shouldBe 5
            }

            test("removing a counter searches for a land card and puts it into hand") {
                val game = castSeekers(forests = 9)
                val seekers = game.findPermanent("Realm Seekers")!!
                val ability = cardRegistry.getCard("Realm Seekers")!!.activatedAbilities.single()

                val activation = game.execute(ActivateAbility(game.player1Id, seekers, ability.id))
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                withClue("the counter is paid as a cost") { plusOneCounters(game) shouldBe 4 }

                game.resolveStack()
                val decision = game.state.pendingDecision as SelectCardsDecision
                val forest = game.findCardsInLibrary(1, "Forest").single()
                withClue("only the land card is a legal choice") { decision.options shouldBe listOf(forest) }
                game.selectCards(listOf(forest)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Forest") shouldBe true
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }
    }
}
