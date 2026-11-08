package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Eternal Scourge (EMN #7, {3}, 3/3 Eldrazi Horror).
 *
 *   You may cast this card from exile.
 *   When this creature becomes the target of a spell or ability an opponent controls, exile this creature.
 */
class EternalScourgeScenarioTest : ScenarioTestBase() {

    init {
        test("an opponent's targeted spell exiles it, and its owner casts it again from exile") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Eternal Scourge")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val scourge = game.findPermanent("Eternal Scourge")!!

            game.castSpell(2, "Lightning Bolt", scourge).error shouldBe null
            game.resolveStack()

            withClue("the trigger exiled Eternal Scourge before Lightning Bolt could resolve") {
                game.findPermanent("Eternal Scourge") shouldBe null
                game.isInExile(1, "Eternal Scourge") shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.castSpellFromExile(1, "Eternal Scourge").error shouldBe null
            game.resolveStack()
            withClue("cast from exile for its mana cost") {
                game.findPermanent("Eternal Scourge") shouldNotBe null
            }
        }

        test("its controller's own targeting doesn't exile it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Eternal Scourge")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val scourge = game.findPermanent("Eternal Scourge")!!

            game.castSpell(1, "Lightning Bolt", scourge).error shouldBe null
            game.resolveStack()

            withClue("Lightning Bolt resolved and killed it: graveyard, not exile") {
                game.isInExile(1, "Eternal Scourge") shouldBe false
                game.isInGraveyard(1, "Eternal Scourge") shouldBe true
            }
        }

        test("exiled face down it has no characteristics, so it can't be cast from exile (CR 406.3a)") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInExile(1, "Eternal Scourge")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val scourgeId = game.state.getExile(game.player1Id).single()
            game.state = game.state.withEntity(scourgeId, game.state.getEntity(scourgeId)!!.with(FaceDownComponent))

            game.castSpellFromExile(1, "Eternal Scourge").error shouldNotBe null
            game.isInExile(1, "Eternal Scourge") shouldBe true
        }
    }
}
