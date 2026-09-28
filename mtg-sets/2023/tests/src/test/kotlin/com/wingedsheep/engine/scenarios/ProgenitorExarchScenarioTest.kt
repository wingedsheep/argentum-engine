package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.ProgenitorExarch
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Progenitor Exarch (MOM #32) — "When this creature enters, incubate 3 X times. {T}: Transform
 * target Incubator token you control."
 */
class ProgenitorExarchScenarioTest : ScenarioTestBase() {

    private val transformAbility = ProgenitorExarch.activatedAbilities[0].id

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(plains: Int, readyExarch: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Progenitor Exarch")
        .withLandsOnBattlefield(1, "Plains", plains)
        .apply { if (readyExarch) withCardOnBattlefield(1, "Progenitor Exarch", summoningSickness = false) }
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Progenitor Exarch") {

            test("X = 2 creates two separate Incubator tokens with three +1/+1 counters each") {
                val game = board(plains = 5)
                game.castXSpell(1, "Progenitor Exarch", xValue = 2).error shouldBe null
                game.resolveStack()

                val incubators = game.findAllPermanents("Incubator")
                incubators shouldHaveSize 2
                incubators.forEach { withClue("each Incubator has 3 counters") { plusOnes(game, it) shouldBe 3 } }
            }

            test("X = 0 incubates zero times") {
                val game = board(plains = 1)
                game.castXSpell(1, "Progenitor Exarch", xValue = 0).error shouldBe null
                game.resolveStack()

                game.findPermanent("Progenitor Exarch") shouldNotBe null
                game.findAllPermanents("Incubator").shouldBeEmpty()
            }

            test("{T}: transforms target Incubator token you control") {
                val game = board(plains = 3, readyExarch = true)
                val ready = game.findPermanent("Progenitor Exarch")!!
                game.castXSpell(1, "Progenitor Exarch", xValue = 1).error shouldBe null
                game.resolveStack()

                val incubator = game.findPermanent("Incubator")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ready,
                        abilityId = transformAbility,
                        targets = listOf(ChosenTarget.Permanent(incubator))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the Incubator flipped to its 0/0 Phyrexian face with its three counters") {
                    game.findPermanent("Incubator") shouldBe null
                    game.findPermanent("Phyrexian") shouldBe incubator
                    plusOnes(game, incubator) shouldBe 3
                }
            }
        }
    }
}
