package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class RuinousUltimatumScenarioTest : ScenarioTestBase() {
    init {
        test("destroys opponents' nonlands while preserving your permanents, lands and indestructible permanents") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Ruinous Ultimatum")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Howling Mine")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Sol Ring")
                .withCardOnBattlefield(2, "Crusade")
                .withCardOnBattlefield(2, "Darksteel Myr")
                .withCardOnBattlefield(2, "Forest")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Ruinous Ultimatum").first()
            game.execute(CastSpell(game.player1Id, spell)).error.shouldBeNull()
            game.resolveStack()

            withClue("opponent creatures, artifacts and enchantments are destroyed") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Sol Ring") shouldBe true
                game.isInGraveyard(2, "Crusade") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Sol Ring") shouldBe false
                game.isOnBattlefield("Crusade") shouldBe false
            }
            withClue("your creatures and artifacts survive") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Howling Mine") shouldBe true
            }
            withClue("lands are excluded and indestructibility is respected") {
                game.isOnBattlefield("Forest") shouldBe true
                game.isOnBattlefield("Plains") shouldBe true
                game.isOnBattlefield("Swamp") shouldBe true
                game.isOnBattlefield("Mountain") shouldBe true
                game.isOnBattlefield("Darksteel Myr") shouldBe true
            }
            game.isInGraveyard(1, "Ruinous Ultimatum") shouldBe true
        }
    }
}
