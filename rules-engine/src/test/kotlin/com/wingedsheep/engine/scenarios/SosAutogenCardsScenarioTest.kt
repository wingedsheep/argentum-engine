package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the trickier Secrets of Strixhaven cards added via the mtgish auto-generator —
 * the shapes whose correctness isn't obvious from the compiled card tree:
 *
 *  - Artistic Process: modal sorcery; mode 2 deals damage to "each creature you DON'T control".
 *    (Guards the emitter fix that renders `Other(You)` as `opponentControls`, not `youControl`.)
 *  - Glorious Decay: modal instant; the "deals 4 damage to target creature with flying" mode.
 *  - Efflorescence: Infusion instant — two +1/+1 counters, plus a conditional keyword grant.
 *  - Withering Curse: Infusion sorcery — all creatures get -2/-2, OR destroy all instead.
 *  - Poisoner's Apprentice: Infusion ETB — intervening-if -4/-4 on an opponent's creature.
 */
class SosAutogenCardsScenarioTest : ScenarioTestBase() {

    init {
        context("Artistic Process — modal sorcery") {

            test("mode 1: 6 damage to a single target creature kills it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Artistic Process")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpellWithMode(1, "Artistic Process", modeIndex = 0, targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("3/3 took 6 damage and died") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("mode 2: 2 damage to each creature you DON'T control spares your own creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Artistic Process")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 — yours, must survive
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 — opponent's, must die
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanents("Grizzly Bears")
                    .first { game.state.projectedState.getController(it) == game.player1Id }

                game.castSpellWithMode(1, "Artistic Process", modeIndex = 1).error shouldBe null
                game.resolveStack()

                withClue("the opponent's 2/2 took 2 damage and died") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("your own 2/2 was NOT dealt damage (you don't control = opponents only)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.projectedState.getController(mine) shouldBe game.player1Id
                }
            }
        }

        context("Glorious Decay — modal instant") {

            test("flying mode: 4 damage to a flying creature kills it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Glorious Decay")
                    .withCardOnBattlefield(2, "Storm Crow") // 1/2 flyer
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crow = game.findPermanent("Storm Crow")!!
                game.castSpellWithMode(1, "Glorious Decay", modeIndex = 1, targetId = crow).error shouldBe null
                game.resolveStack()

                withClue("the flyer took 4 damage and died") {
                    game.isInGraveyard(2, "Storm Crow") shouldBe true
                }
            }
        }

        context("Efflorescence — Infusion instant") {

            test("without life gained: two +1/+1 counters, no keyword grant") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Efflorescence")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Efflorescence", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("two +1/+1 counters were placed") {
                    val counters = game.state.getEntity(bears)
                        ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 2
                }
                withClue("no life gained this turn → no trample/indestructible") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
                }
            }

            test("after gaining life: two counters AND trample + indestructible") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Efflorescence")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(3))
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Efflorescence", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("two +1/+1 counters were placed") {
                    val counters = game.state.getEntity(bears)
                        ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 2
                }
                withClue("life gained this turn → trample + indestructible granted") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }
        }

        context("Withering Curse — Infusion sorcery") {

            test("without life gained: all creatures get -2/-2 (3/3 survives as 1/1)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Withering Curse")
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3 -> 1/1 survives
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 -> 0/0 dies
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Withering Curse").error shouldBe null
                game.resolveStack()

                withClue("the 2/2 dropped to 0/0 and died") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("the 3/3 survives as a 1/1") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.state.projectedState.getToughness(giant) shouldBe 1
                }
            }

            test("after gaining life: destroy all creatures instead (3/3 also dies)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Withering Curse")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(2))
                }

                game.castSpell(1, "Withering Curse").error shouldBe null
                game.resolveStack()

                withClue("life gained → destroy all: even the 3/3 is destroyed") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }

        context("Poisoner's Apprentice — Infusion ETB") {

            test("after gaining life: an opponent's creature gets -4/-4 and dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Poisoner's Apprentice")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 -> -2/-2 dies
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(2))
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Poisoner's Apprentice").error shouldBe null
                game.resolveStack() // creature resolves; the ETB intervening-if fires and asks for a target
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's 2/2 took -4/-4 and died") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("without life gained: the ETB ability does not fire") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Poisoner's Apprentice")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Poisoner's Apprentice").error shouldBe null
                game.resolveStack()

                withClue("no life gained → intervening-if false → creature untouched") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
