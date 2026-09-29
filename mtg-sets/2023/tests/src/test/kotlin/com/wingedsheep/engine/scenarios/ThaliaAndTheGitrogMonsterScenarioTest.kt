package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thalia and The Gitrog Monster {1}{W}{B}{G} — March of the Machine #255.
 *
 * Covers the additional land drop, the opponents-only enter-tapped replacement (creatures and
 * nonbasic lands, not basics), and the mandatory attack-trigger sacrifice followed by a draw.
 */
class ThaliaAndTheGitrogMonsterScenarioTest : ScenarioTestBase() {

    private fun TestGame.tapped(id: EntityId): Boolean = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Thalia and The Gitrog Monster") {

            test("its controller may play two lands, but not three") {
                val game = scenario()
                    .withPlayers("Thalia", "Opponent")
                    .withCardOnBattlefield(1, "Thalia and The Gitrog Monster")
                    .withCardsInHand(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forests = game.findCardsInHand(1, "Forest")
                game.execute(PlayLand(game.player1Id, forests[0])).error shouldBe null
                game.execute(PlayLand(game.player1Id, forests[1])).error shouldBe null
                withClue("one extra land drop, not two") {
                    game.execute(PlayLand(game.player1Id, forests[2])).error shouldNotBe null
                }
            }

            test("opponents' nonbasic lands and creatures enter tapped") {
                val game = scenario()
                    .withPlayers("Thalia", "Opponent")
                    .withCardOnBattlefield(1, "Thalia and The Gitrog Monster")
                    .withCardInHand(2, "Desert")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val desert = game.findCardsInHand(2, "Desert").single()
                game.execute(PlayLand(game.player2Id, desert)).error shouldBe null
                game.tapped(desert) shouldBe true

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.tapped(bears) shouldBe true
            }

            test("opponents' basic lands enter untapped, and its controller's permanents are unaffected") {
                val game = scenario()
                    .withPlayers("Thalia", "Opponent")
                    .withCardOnBattlefield(1, "Thalia and The Gitrog Monster")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findCardsInHand(2, "Forest").single()
                game.execute(PlayLand(game.player2Id, forest)).error shouldBe null
                game.tapped(forest) shouldBe false
            }

            test("its controller's own nonbasic land enters untapped") {
                val game = scenario()
                    .withPlayers("Thalia", "Opponent")
                    .withCardOnBattlefield(1, "Thalia and The Gitrog Monster")
                    .withCardInHand(1, "Desert")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val desert = game.findCardsInHand(1, "Desert").single()
                game.execute(PlayLand(game.player1Id, desert)).error shouldBe null
                game.tapped(desert) shouldBe false
            }

            test("attacking forces a creature-or-land sacrifice, then draws a card") {
                val game = scenario()
                    .withPlayers("Thalia", "Opponent")
                    .withCardOnBattlefield(1, "Thalia and The Gitrog Monster", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest").shouldNotBeNull()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thalia and The Gitrog Monster" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the sacrifice isn't optional; Thalia or the Forest are the choices") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(forest))
                game.resolveStack()

                game.isOnBattlefield("Forest") shouldBe false
                withClue("a non-creature, non-land permanent is never a candidate") {
                    game.isOnBattlefield("Glorious Anthem") shouldBe true
                }
                game.isOnBattlefield("Thalia and The Gitrog Monster") shouldBe true
                game.handSize(1) shouldBe 1
            }
        }
    }
}
