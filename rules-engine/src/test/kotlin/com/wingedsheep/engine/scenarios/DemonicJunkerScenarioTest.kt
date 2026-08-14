package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Demonic Junker (DFT #83) — {6}{B} Artifact — Vehicle (4/3).
 *
 * "Affinity for artifacts
 *  When this Vehicle enters, for each player, destroy up to one target creature that player
 *  controls. If a creature you controlled was destroyed this way, put two +1/+1 counters on this
 *  Vehicle.
 *  Crew 2"
 *
 * The interesting clause is the payoff: it asks whether a creature you controlled was *destroyed*,
 * not whether you targeted one. The pipeline splits the chosen targets by controller before the
 * move, destroys them together, and subtracts the opponent's half from what actually died — so an
 * indestructible pick, or targeting only the opponent, leaves the Vehicle bare.
 */
class DemonicJunkerScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * The two "up to one" slots are separate target requirements — index 0 is your creature,
     * index 1 the opponent's — so each has to be answered on its own key. Pass null to decline a
     * slot.
     */
    private fun chooseVictims(game: TestGame, yours: EntityId?, theirs: EntityId?) {
        val decisionId = game.getPendingDecision()!!.id
        game.submitDecision(
            TargetsResponse(
                decisionId,
                mapOf(0 to listOfNotNull(yours), 1 to listOfNotNull(theirs))
            )
        ).error shouldBe null
    }

    init {
        context("Demonic Junker's enters trigger") {

            test("destroying one creature per player pays off — both die, the Vehicle gets two counters") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Demonic Junker")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Demonic Junker").error shouldBe null
                game.resolveStack()
                chooseVictims(game, yours = bears, theirs = giant)
                game.resolveStack()

                withClue("your own creature was destroyed") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("the opponent's creature was destroyed") { game.isInGraveyard(2, "Hill Giant") shouldBe true }

                val junker = game.findPermanent("Demonic Junker")
                withClue("the Vehicle is on the battlefield") { junker shouldNotBe null }
                withClue("a creature you controlled died ⇒ two +1/+1 counters") {
                    plusOneCounters(game, junker!!) shouldBe 2
                }
            }

            test("destroying only the opponent's creature leaves the Vehicle without counters") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Demonic Junker")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Demonic Junker").error shouldBe null
                game.resolveStack()
                // "up to one" per player — decline the slot pointed at your own board.
                chooseVictims(game, yours = null, theirs = giant)
                game.resolveStack()

                withClue("your creature was never targeted, so it lives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("the opponent's creature was destroyed") { game.isInGraveyard(2, "Hill Giant") shouldBe true }

                val junker = game.findPermanent("Demonic Junker")!!
                withClue("no creature you controlled was destroyed ⇒ no counters") {
                    plusOneCounters(game, junker) shouldBe 0
                }
            }

            test("an indestructible creature of yours is targeted but not destroyed — no counters") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Demonic Junker")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(1, "Zetalpa, Primal Dawn", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zetalpa = game.findPermanent("Zetalpa, Primal Dawn")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Demonic Junker").error shouldBe null
                game.resolveStack()
                chooseVictims(game, yours = zetalpa, theirs = giant)
                game.resolveStack()

                withClue("indestructible — it survives the destroy") {
                    game.isOnBattlefield("Zetalpa, Primal Dawn") shouldBe true
                }
                withClue("the opponent's creature still dies") { game.isInGraveyard(2, "Hill Giant") shouldBe true }

                val junker = game.findPermanent("Demonic Junker")!!
                withClue("\"destroyed this way\" — targeting isn't enough, so no counters") {
                    plusOneCounters(game, junker) shouldBe 0
                }
            }
        }

        context("Affinity for artifacts") {

            test("each artifact you control cuts {1} off the cost") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Demonic Junker")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // {6}{B} is seven mana; two artifacts you control knock it to {4}{B} — exactly the
                // five Swamps on the battlefield, so this cast only happens because of affinity.
                withClue("affinity made it castable off five Swamps") {
                    game.castSpell(1, "Demonic Junker").error shouldBe null
                }
            }
        }
    }
}
