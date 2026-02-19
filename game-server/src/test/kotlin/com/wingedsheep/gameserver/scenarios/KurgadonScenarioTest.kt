package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kurgadon.
 *
 * Card reference:
 * - Kurgadon ({4}{G}): Creature — Beast, 3/3
 *   "Whenever you cast a creature spell with mana value 6 or greater,
 *   put three +1/+1 counters on Kurgadon."
 */
class KurgadonScenarioTest : ScenarioTestBase() {

    init {
        context("Kurgadon") {

            test("gets three +1/+1 counters when controller casts a creature with MV 6+") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kurgadon")
                    .withCardInHand(1, "Whiptail Wurm") // MV 7
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Whiptail Wurm")
                withClue("Casting Whiptail Wurm should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val kurgadonId = game.findPermanent("Kurgadon")!!
                val counters = game.state.getEntity(kurgadonId)?.get<CountersComponent>()
                withClue("Kurgadon should have 3 +1/+1 counters") {
                    counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
            }

            test("does not trigger when casting a creature with MV less than 6") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kurgadon")
                    .withCardInHand(1, "Grizzly Bears") // MV 2
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Casting Grizzly Bears should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val kurgadonId = game.findPermanent("Kurgadon")!!
                val counters = game.state.getEntity(kurgadonId)?.get<CountersComponent>()
                withClue("Kurgadon should have no counters") {
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                }
            }

            test("does not trigger when casting a non-creature spell with MV 6+") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kurgadon")
                    .withCardInHand(1, "Searing Flesh") // MV 7, sorcery (deals 7 to target opponent)
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingPlayer(1, "Searing Flesh", 2)
                withClue("Casting Searing Flesh should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val kurgadonId = game.findPermanent("Kurgadon")!!
                val counters = game.state.getEntity(kurgadonId)?.get<CountersComponent>()
                withClue("Kurgadon should have no counters (Searing Flesh is not a creature spell)") {
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                }
            }

            test("triggers multiple times for multiple big creature casts") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kurgadon")
                    .withCardInHand(1, "Whiptail Wurm")  // MV 7
                    .withCardInHand(1, "Archangel")       // MV 7
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast first big creature
                val cast1 = game.castSpell(1, "Whiptail Wurm")
                withClue("Casting Whiptail Wurm should succeed: ${cast1.error}") {
                    cast1.error shouldBe null
                }
                game.resolveStack()

                val kurgadonId = game.findPermanent("Kurgadon")!!
                val countersAfterFirst = game.state.getEntity(kurgadonId)?.get<CountersComponent>()
                withClue("Kurgadon should have 3 counters after first big creature") {
                    countersAfterFirst?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }

                // Cast second big creature
                val cast2 = game.castSpell(1, "Archangel")
                withClue("Casting Archangel should succeed: ${cast2.error}") {
                    cast2.error shouldBe null
                }
                game.resolveStack()

                val countersAfterSecond = game.state.getEntity(kurgadonId)?.get<CountersComponent>()
                withClue("Kurgadon should have 6 counters after second big creature") {
                    countersAfterSecond?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 6
                }
            }
        }
    }
}
