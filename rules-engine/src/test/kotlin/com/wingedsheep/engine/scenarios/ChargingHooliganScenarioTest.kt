package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Charging Hooligan. */
class ChargingHooliganScenarioTest : ScenarioTestBase() {

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
        context("Charging Hooligan — +1/+0 per attacking creature, trample if a Rat attacks") {
            test("attacking alongside two others is +3/+0 — the count includes the Hooligan itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Charging Hooligan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hooligan = game.findPermanent("Charging Hooligan").shouldNotBeNull()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Charging Hooligan" to 2, "Grizzly Bears" to 2, "Savannah Lions" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("three attackers → +3/+0 on a 3/3") {
                    power(game, hooligan) shouldBe 6
                    toughness(game, hooligan) shouldBe 3
                }
                withClue("no Rat is attacking, so no trample") {
                    game.state.projectedState.hasKeyword(hooligan, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("attacking alone is only +1/+0 — the creatures left home don't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Charging Hooligan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hooligan = game.findPermanent("Charging Hooligan").shouldNotBeNull()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Charging Hooligan" to 2)).error shouldBe null
                game.resolveStack()

                withClue("only the Hooligan attacks — it counts once, for +1/+0") {
                    power(game, hooligan) shouldBe 4
                }
            }

            test("an attacking Rat turns on trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Voracious Vermin is a Rat, so it satisfies "if a Rat is attacking".
                    .withCardOnBattlefield(1, "Charging Hooligan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Voracious Vermin", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hooligan = game.findPermanent("Charging Hooligan").shouldNotBeNull()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Charging Hooligan" to 2, "Voracious Vermin" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("two attackers → +2/+0") { power(game, hooligan) shouldBe 5 }
                withClue("the Rat is attacking, so trample is granted") {
                    game.state.projectedState.hasKeyword(hooligan, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("a Rat you control that stayed home does not grant trample — it must be attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Charging Hooligan", summoningSickness = false)
                    .withCardOnBattlefield(1, "Voracious Vermin", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hooligan = game.findPermanent("Charging Hooligan").shouldNotBeNull()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Charging Hooligan" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the Rat is on the battlefield but not attacking") {
                    game.state.projectedState.hasKeyword(hooligan, Keyword.TRAMPLE) shouldBe false
                }
            }
        }
    }
}
