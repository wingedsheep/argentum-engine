package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ArgentDais
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Argent Dais (MH3 #20): "This artifact enters with two oil counters on it. Whenever two or more
 * creatures attack, put an oil counter on this artifact. {2}, {T}, Remove two oil counters from
 * this artifact: Exile another target nonland permanent. Its controller draws two cards."
 *
 * Ruling: the attack trigger fires whenever two or more creatures attack, not just when you attack.
 */
class ArgentDaisScenarioTest : ScenarioTestBase() {

    private fun TestGame.oil(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    init {
        context("Argent Dais") {

            test("enters with two oil counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Argent Dais")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Argent Dais").error shouldBe null
                game.resolveStack()

                game.oil(game.findPermanent("Argent Dais")!!) shouldBe 2
            }

            test("an opponent attacking with two creatures adds an oil counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Argent Dais")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val dais = game.findPermanent("Argent Dais")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 1, "Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()

                game.oil(dais) shouldBe 1
            }

            test("you attacking with two creatures adds an oil counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Argent Dais")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val dais = game.findPermanent("Argent Dais")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                game.oil(dais) shouldBe 1
            }

            test("a single attacker does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Argent Dais")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val dais = game.findPermanent("Argent Dais")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
                game.resolveStack()

                game.oil(dais) shouldBe 0
            }

            test("removing two oil counters exiles a nonland permanent and its controller draws two") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Argent Dais")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Argent Dais").error shouldBe null
                game.resolveStack()
                val dais = game.findPermanent("Argent Dais")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val opponentHand = game.handSize(2)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = dais,
                        abilityId = ArgentDais.activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Centaur Courser") shouldBe true
                game.handSize(2) shouldBe opponentHand + 2
                game.oil(dais) shouldBe 0
            }

            test("can't activate with fewer than two oil counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Argent Dais")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Argent Dais")!!,
                        abilityId = ArgentDais.activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Centaur Courser")!!))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
