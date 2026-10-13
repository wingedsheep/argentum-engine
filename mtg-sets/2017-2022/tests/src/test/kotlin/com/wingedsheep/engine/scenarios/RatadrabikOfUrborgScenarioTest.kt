package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Ratadrabik of Urborg (DMU #213) — "Whenever another legendary creature you control dies, create a
 * token that's a copy of that creature, except it's not legendary and it's a 2/2 black Zombie in
 * addition to its other colors and types."
 *
 * The token copies the departed creature's last-known copiable values (it is already in the
 * graveyard when the trigger resolves) and every exception lands: legendary stripped (so the legend
 * rule leaves it alone), base 2/2 over the printed 1/1, black added to red, Zombie added to Goblin.
 * Being a Zombie, it also picks up Ratadrabik's "Other Zombies you control have vigilance."
 */
class RatadrabikOfUrborgScenarioTest : ScenarioTestBase() {

    private fun board(creature: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Ratadrabik of Urborg")
        .withCardOnBattlefield(1, creature)
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a dying legend comes back as a nonlegendary 2/2 black Zombie token copy") {
            val game = board("Squee, Goblin Nabob")
            val squee = game.findPermanent("Squee, Goblin Nabob")!!
            game.castSpell(1, "Shock", squee).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Squee, Goblin Nabob") shouldBe true
            val token = game.findPermanent("Squee, Goblin Nabob")!!
            val projected = game.state.projectedState
            withClue("token copy with the exceptions applied") {
                game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
                projected.isLegendary(token) shouldBe false
                projected.getPower(token) shouldBe 2
                projected.getToughness(token) shouldBe 2
                projected.getColors(token) shouldContainAll setOf("RED", "BLACK")
                projected.getSubtypes(token) shouldContainAll setOf("Goblin", "Zombie")
                projected.isCreature(token) shouldBe true
            }
            withClue("as an other Zombie, the token gets vigilance from Ratadrabik") {
                projected.hasKeyword(token, Keyword.VIGILANCE) shouldBe true
            }
        }

        test("a nonlegendary creature dying makes no token") {
            val game = board("Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Shock", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.findPermanent("Grizzly Bears") shouldBe null
        }
    }
}
