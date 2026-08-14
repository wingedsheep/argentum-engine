package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Galvanize (MKM) — "Galvanize deals 3 damage to target creature. If you've drawn two or more
 * cards this turn, Galvanize deals 5 damage to that creature instead."
 *
 * "Instead" replaces the amount, so this is a single damage event whose size is a
 * `DynamicAmount.Conditional` over `Conditions.YouDrewCardsThisTurn(2)` — not 3 damage plus a
 * conditional 2 more. The threshold is *your* draws, not the table's.
 */
class GalvanizeScenarioTest : ScenarioTestBase() {

    private fun damageOn(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        context("Galvanize — 3 damage, or 5 after two draws") {

            test("deals 3 damage when you have drawn nothing this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanize")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanize", targetId = wurm).error shouldBe null
                game.resolveStack()

                withClue("3 damage is not lethal to a 6/4") {
                    damageOn(game, wurm) shouldBe 3
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
            }

            test("deals 3 damage after only one draw — the threshold is two") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanize")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanize", targetId = wurm).error shouldBe null
                game.resolveStack()

                damageOn(game, wurm) shouldBe 3
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }

            test("deals 5 damage — lethal to the 6/4 — once you have drawn two cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanize")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardsDrawnThisTurn(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanize", targetId = wurm).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("5 damage on a toughness-4 creature is lethal") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }
            }

            test("the opponent's draws do not raise the amount") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanize")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardsDrawnThisTurn(2, 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanize", targetId = wurm).error shouldBe null
                game.resolveStack()

                withClue("'you've drawn' is controller-scoped") {
                    damageOn(game, wurm) shouldBe 3
                }
            }
        }
    }
}
