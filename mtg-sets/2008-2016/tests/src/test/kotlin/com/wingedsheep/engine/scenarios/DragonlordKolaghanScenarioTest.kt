package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dragonlord Kolaghan — "Whenever an opponent casts a creature or planeswalker spell with the same
 * name as a card in their graveyard, that player loses 10 life." The name is compared against the
 * *caster's* graveyard, never Kolaghan's controller's; a face-down spell has no name (DTK ruling).
 */
class DragonlordKolaghanScenarioTest : ScenarioTestBase() {

    init {
        context("Dragonlord Kolaghan — same-name punisher") {
            fun opponentCasts(
                spell: String,
                opponentGraveyard: List<String> = emptyList(),
                ourGraveyard: List<String> = emptyList(),
            ) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dragonlord Kolaghan", summoningSickness = false)
                .withCardInHand(2, spell)
                .withLandsOnBattlefield(2, "Forest", 6)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { opponentGraveyard.forEach { withCardInGraveyard(2, it) } }
                .apply { ourGraveyard.forEach { withCardInGraveyard(1, it) } }
                .build()

            test("an opponent casting a creature named like a card in their graveyard loses 10 life") {
                val game = opponentCasts("Grizzly Bears", opponentGraveyard = listOf("Grizzly Bears"))

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("the caster lost 10") { game.getLifeTotal(2) shouldBe 10 }
                withClue("Kolaghan's controller is untouched") { game.getLifeTotal(1) shouldBe 20 }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("a same-named card in Kolaghan's controller's graveyard doesn't count") {
                val game = opponentCasts("Grizzly Bears", ourGraveyard = listOf("Grizzly Bears"))

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("a creature with no same-named card in their graveyard doesn't trigger") {
                val game = opponentCasts("Grizzly Bears", opponentGraveyard = listOf("Hill Giant"))

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("a noncreature, nonplaneswalker spell doesn't trigger even with a same-named card") {
                val game = opponentCasts("Lightning Bolt", opponentGraveyard = listOf("Lightning Bolt"))
                val kolaghan = game.findPermanent("Dragonlord Kolaghan")!!

                game.castSpell(2, "Lightning Bolt", kolaghan).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("a creature spell cast face down has no name and doesn't trigger") {
                val game = opponentCasts("Aerie Bowmasters", opponentGraveyard = listOf("Aerie Bowmasters"))
                val cardId = game.findCardsInHand(2, "Aerie Bowmasters").first()

                game.execute(CastSpell(game.player2Id, cardId, emptyList(), castFaceDown = true)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("Kolaghan's own controller casting a same-named creature doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragonlord Kolaghan", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("Dragonlord Kolaghan — other creatures you control have haste") {
            test("grants haste to your other creatures only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragonlord Kolaghan")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = true)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.HASTE) shouldBe true
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.HASTE) shouldBe false
                withClue("Kolaghan has haste printed") {
                    projected.hasKeyword(game.findPermanent("Dragonlord Kolaghan")!!, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
