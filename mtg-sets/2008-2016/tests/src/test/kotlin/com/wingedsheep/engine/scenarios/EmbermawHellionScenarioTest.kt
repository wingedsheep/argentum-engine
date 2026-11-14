package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Embermaw Hellion — "If another red source you control would deal damage to a permanent or
 * player, it deals that much damage plus 1 to that permanent or player instead."
 *
 * Pins the scopes the snapshot can't see: a red *spell* is boosted (not only creatures), the bonus
 * reaches permanents and you yourself, "another" keeps the Hellion's own combat damage at 4, a
 * non-red source isn't boosted, and a second Hellion boosts the first (cumulative ruling).
 */
class EmbermawHellionScenarioTest : ScenarioTestBase() {

    private fun board(vararg opponentPermanents: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Embermaw Hellion")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .apply { opponentPermanents.forEach { withCardOnBattlefield(2, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun combat(vararg attackers: String, secondHellion: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Embermaw Hellion", summoningSickness = false)
        .apply { if (secondHellion) withCardOnBattlefield(1, "Embermaw Hellion") }
        .apply { attackers.forEach { withCardOnBattlefield(1, it, summoningSickness = false) } }
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a red spell you control deals 1 extra damage to an opponent") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
        }

        test("the bonus applies to damage to you as well — any player") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }

        test("the bonus applies to damage to a permanent") {
            val game = board("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Shock", targetId = giant).error shouldBe null
            game.resolveStack()
            withClue("Shock deals 3 to the 3/3 Hill Giant") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        test("the Hellion's own combat damage isn't boosted, another red creature's is") {
            val game = combat("Raging Goblin")
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Embermaw Hellion" to 2, "Raging Goblin" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("Hellion deals 4, Raging Goblin deals 1 + 1") { game.getLifeTotal(2) shouldBe 14 }
        }

        test("a non-red source you control isn't boosted") {
            val game = combat("Grizzly Bears")
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
        }

        test("a second Hellion boosts the first — multiple Hellions are cumulative") {
            val game = combat(secondHellion = true)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val attacker = game.findPermanent("Embermaw Hellion")!!
            game.execute(
                DeclareAttackers(game.player1Id, mapOf(attacker to game.player2Id))
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("the attacking Hellion's 4 gets +1 from the other Hellion, not from itself") {
                game.getLifeTotal(2) shouldBe 15
            }
        }
    }
}
