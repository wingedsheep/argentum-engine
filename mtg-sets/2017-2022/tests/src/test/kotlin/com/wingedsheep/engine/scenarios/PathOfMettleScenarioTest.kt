package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.PathOfMettle
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Path of Mettle // Metzali, Tower of Triumph (RIX #165).
 *
 * Pins the ETB sweep's "doesn't have any of the four keywords" filter, the transform trigger's
 * "two attackers that each have any one of the keywords" count (they needn't share one, and a
 * plain attacker doesn't count toward the two), and Metzali's random destroy drawing only from
 * creatures that attacked this turn.
 */
class PathOfMettleScenarioTest : ScenarioTestBase() {

    private fun attackGame(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Path of Mettle")
        .withCardOnBattlefield(1, "Raging Goblin")
        .withCardOnBattlefield(1, "Youthful Knight")
        .withCardOnBattlefield(1, "Llanowar Elves")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.pathName(): String? =
        state.getEntity(findPermanent("Path of Mettle") ?: findPermanent("Metzali, Tower of Triumph")!!)
            ?.get<CardComponent>()?.name

    init {
        test("enters dealing 1 damage only to creatures with none of the four keywords") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Path of Mettle")
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardOnBattlefield(1, "Youthful Knight")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Path of Mettle").error shouldBe null
            game.resolveStack()

            withClue("the 1/1 with no listed keyword dies") {
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            }
            withClue("the 1/1 with haste is spared") {
                game.isOnBattlefield("Raging Goblin") shouldBe true
            }
            withClue("the first striker is spared") {
                game.isOnBattlefield("Youthful Knight") shouldBe true
            }
        }

        test("transforms when two attackers each have a different listed keyword") {
            val game = attackGame()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Raging Goblin" to 2, "Youthful Knight" to 2)).error shouldBe null
            game.resolveStack()

            game.pathName() shouldBe "Metzali, Tower of Triumph"
        }

        test("doesn't transform when only one attacker has a listed keyword") {
            val game = attackGame()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Raging Goblin" to 2, "Llanowar Elves" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.resolveStack()

            game.pathName() shouldBe "Path of Mettle"
        }

        test("Metzali destroys a random creature among those that attacked this turn") {
            val game = attackGame()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Raging Goblin" to 2, "Youthful Knight" to 2)).error shouldBe null
            game.resolveStack()
            val metzali = game.findPermanent("Metzali, Tower of Triumph")!!

            val destroy = PathOfMettle.backFace!!.activatedAbilities[2].id
            game.execute(ActivateAbility(game.player1Id, metzali, destroy)).error shouldBe null
            game.resolveStack()

            val attackersDestroyed = listOf("Raging Goblin", "Youthful Knight").count { game.isInGraveyard(1, it) }
            attackersDestroyed shouldBe 1
            withClue("creatures that didn't attack are never chosen") {
                game.isOnBattlefield("Llanowar Elves") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
