package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Moonmist — "Transform all Humans. Prevent all combat damage that would be dealt this turn by
 * creatures other than Werewolves and Wolves."
 *
 * Pins that a double-faced Human transforms (Mayor of Avabruck → Howlpack Alpha), a single-faced
 * Human is untouched (CR 701.27c), and that only Werewolves/Wolves deal combat damage afterwards.
 */
class MoonmistScenarioTest : ScenarioTestBase() {

    init {
        test("transforms double-faced Humans and prevents combat damage from non-Werewolf non-Wolf creatures") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Moonmist")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Mayor of Avabruck")
                .withCardOnBattlefield(1, "Elite Inquisitor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mayor = game.findPermanent("Mayor of Avabruck")!!
            val inquisitor = game.findPermanent("Elite Inquisitor")!!

            val result = game.castSpell(1, "Moonmist")
            withClue("cast succeeds: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("the double-faced Human transformed into its Werewolf face") {
                game.state.getEntity(mayor)!!.get<CardComponent>()!!.name shouldBe "Howlpack Alpha"
            }
            withClue("a single-faced Human is unaffected") {
                game.state.getEntity(inquisitor)!!.get<CardComponent>()!!.name shouldBe "Elite Inquisitor"
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Howlpack Alpha" to 2, "Elite Inquisitor" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("only the Werewolf's 3 combat damage is dealt") {
                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
