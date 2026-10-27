package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Serra's Emissary (MH2 #30).
 *
 * "Flying / As this creature enters, choose a card type. / You and creatures you control have
 *  protection from the chosen card type."
 *
 * The as-enters choice feeds two protections through `ProtectionScope.ChosenCardType`: the
 * creatures' (a projected `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword) and the controller's (player
 * protection). Protection from a card type isn't controller-relative, so the Emissary's own
 * controller's instants are stopped too.
 */
class SerrasEmissaryScenarioTest : ScenarioTestBase() {

    private val emissary = "Serra's Emissary"

    private fun TestGame.castEmissaryChoosing(type: String) {
        castSpell(1, emissary).error shouldBe null
        resolveStack()
        val decision = getPendingDecision() as? ChooseOptionDecision
            ?: error("Expected the Emissary's card-type choice, got ${getPendingDecision()}")
        withClue("every card type a normal game can produce is offered") {
            decision.options shouldContainAll listOf(
                "Artifact", "Battle", "Creature", "Enchantment", "Instant",
                "Kindred", "Land", "Planeswalker", "Sorcery"
            )
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(type))).error shouldBe null
        resolveStack()
        isOnBattlefield(emissary) shouldBe true
    }

    private fun boardWith(vararg inHand: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, emissary)
        .apply { inHand.forEach { withCardInHand(1, it) } }
        .withLandsOnBattlefield(1, "Plains", 7)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("instant chosen: no instant can target you, the Emissary or your other creatures") {
            val game = boardWith("Lightning Bolt").build()
            game.castEmissaryChoosing("Instant")

            withClue("you have protection from instants") {
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldNotBe null
            }
            withClue("the Emissary has protection from instants") {
                game.castSpell(1, "Lightning Bolt", game.findPermanent(emissary)).error shouldNotBe null
            }
            withClue("another creature you control has protection from instants") {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")).error shouldNotBe null
            }
            withClue("an opponent's creature isn't protected") {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")).error shouldBe null
            }
        }

        test("sorcery chosen: instants are unaffected") {
            val game = boardWith("Lightning Bolt").build()
            game.castEmissaryChoosing("Sorcery")

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }

        test("creature chosen: you are protected from creature sources, and your creatures can't be blocked by creatures") {
            val game = boardWith().build()
            game.castEmissaryChoosing("Creature")

            withClue("an opponent's creature is a source you're protected from") {
                PlayerProtectionRules.isProtectedFromSource(
                    game.state, game.player1Id, game.findPermanent("Hill Giant"), game.player2Id,
                    predicateEvaluator = services.predicateEvaluator
                ) shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("protection from creatures: Hill Giant can't block Grizzly Bears") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
        }
    }
}
