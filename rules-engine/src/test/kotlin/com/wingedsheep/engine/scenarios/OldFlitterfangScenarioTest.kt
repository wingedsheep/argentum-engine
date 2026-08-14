package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.woe.cards.OldFlitterfang
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Old Flitterfang. */
class OldFlitterfangScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    /**
     * Twisted Fealty has two independent targets, which the base fixture's single-target
     * [ScenarioTestBase.TestGame.castSpell] can't express — cast it directly so the "up to one"
     * second target can be present or absent.
     */
    private fun TestGame.castTwistedFealty(targets: List<EntityId>) = execute(
        CastSpell(
            player1Id,
            findCardsInHand(1, "Twisted Fealty").first(),
            targets.map { ChosenTarget.Permanent(it) }
        )
    )

    init {
        context("Old Flitterfang") {
            test("with a creature dead this turn, the end step makes a Food token") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Flitterfang", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpell(1, "Doom Blade", targetId = bears).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("a creature died this turn, so the intervening 'if' is satisfied") {
                    game.findPermanent("Food").shouldNotBeNull()
                }
            }

            test("with nothing dead, the ability never triggers — no Food") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Flitterfang", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("nothing died, so no Food is created") {
                    game.findPermanent("Food") shouldBe null
                }
            }

            test("sacrificing another artifact for +2/+2 leaves Old Flitterfang itself alone") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Flitterfang", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val flitterfang = game.findPermanent("Old Flitterfang").shouldNotBeNull()
                val thopter = game.findPermanent("Ornithopter").shouldNotBeNull()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = flitterfang,
                        abilityId = OldFlitterfang.activatedAbilities[0].id
                    )
                ).error shouldBe null
                // The sacrifice is a cost, so it is paid on activation: pick the Ornithopter.
                if (game.hasPendingDecision()) {
                    game.selectCards(listOf(thopter)).error shouldBe null
                }
                game.resolveStack()

                withClue("the Ornithopter was eaten, not the Flitterfang") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                    game.isOnBattlefield("Old Flitterfang") shouldBe true
                }
                withClue("the 3/4 becomes 5/6 until end of turn") {
                    power(game, flitterfang) shouldBe 5
                    toughness(game, flitterfang) shouldBe 6
                }
            }
        }
    }
}
