package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario coverage for Sculpting Steel (MRD #238).
 *
 * {3} Artifact
 * "You may have this artifact enter as a copy of any artifact on the battlefield."
 *
 * Clone narrowed to artifacts. What's worth proving: "any artifact" reaches across the table,
 * non-artifacts are not on the menu, and the "may" is a real decline that leaves a vanilla {3}
 * artifact behind rather than fizzling the permanent out of existence.
 */
class SculptingSteelScenarioTest : ScenarioTestBase() {

    init {
        context("Sculpting Steel") {

            test("copies an opponent's artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Gilded Lotus")
                    .withCardInHand(1, "Sculpting Steel")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lotus = game.findPermanent("Gilded Lotus")!!
                val steel = game.findCardsInHand(1, "Sculpting Steel").single()

                game.castSpell(1, "Sculpting Steel").error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("'any artifact on the battlefield' includes the opponent's") {
                    decision.options shouldContain lotus
                }
                game.selectCards(listOf(lotus))

                val card = game.state.getEntity(steel)?.get<CardComponent>()
                withClue("the copy takes on the copied artifact's characteristics") {
                    card shouldNotBe null
                    card!!.name shouldBe "Gilded Lotus"
                    card.typeLine.isArtifact shouldBe true
                }
                val copyOf = game.state.getEntity(steel)?.get<CopyOfComponent>()
                copyOf shouldNotBe null
                copyOf!!.originalCardDefinitionId shouldBe "Sculpting Steel"
                copyOf.copiedCardDefinitionId shouldBe "Gilded Lotus"
            }

            test("non-artifacts are never offered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Sculpting Steel")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bonesplitter = game.findPermanent("Bonesplitter")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Sculpting Steel").error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldContain bonesplitter
                withClue("Hill Giant is a creature, not an artifact") {
                    decision.options shouldNotContain giant
                }
            }

            test("declining the copy leaves a plain {3} artifact on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Gilded Lotus")
                    .withCardInHand(1, "Sculpting Steel")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val steel = game.findCardsInHand(1, "Sculpting Steel").single()

                game.castSpell(1, "Sculpting Steel").error shouldBe null
                game.resolveStack()

                game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection()

                withClue("the 'may' is declinable — it still enters, just as itself") {
                    game.findPermanent("Sculpting Steel") shouldBe steel
                    game.state.getEntity(steel)?.get<CopyOfComponent>() shouldBe null
                    game.findPermanents("Gilded Lotus").size shouldBe 1
                }
            }
        }
    }
}
