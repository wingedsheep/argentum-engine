package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Alhammarret, High Arbiter (ORI #43) — "As Alhammarret enters, each opponent reveals their hand.
 * You choose the name of a nonland card revealed this way. Your opponents can't cast spells with the
 * chosen name (as long as this creature is on the battlefield)."
 *
 * Rulings (2015-06-22): one name, not one per opponent; with no nonland card revealed no name is
 * chosen and nothing is stopped; a card with the chosen name can still be cast face down (morph).
 */
class AlhammarretHighArbiterScenarioTest : ScenarioTestBase() {

    private fun TestGame.pendingChoice(): ChooseOptionDecision {
        val decision = getPendingDecision()
        withClue("Alhammarret asks for a name as it enters") { (decision is ChooseOptionDecision) shouldBe true }
        return decision as ChooseOptionDecision
    }

    private fun TestGame.choose(decision: ChooseOptionDecision, option: String) =
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(option)))

    private fun TestGame.alhammarret() = findPermanent("Alhammarret, High Arbiter")!!

    init {
        test("offers only the nonland names in the opponent's revealed hand, and bans the chosen one for opponents") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Alhammarret, High Arbiter")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 7)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Forest")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Alhammarret, High Arbiter").error shouldBe null
            game.resolveStack()
            val decision = game.pendingChoice()
            withClue("only nonland cards revealed from the opponent's hand, each name once") {
                decision.options shouldContainExactly listOf("Grizzly Bears", "Lightning Bolt")
            }
            withClue("the opponent's whole hand, the land too, is revealed to every player") {
                game.state.getHand(game.player2Id).forEach { card ->
                    val revealed = game.state.getEntity(card)?.get<RevealedToComponent>()
                    revealed?.isRevealedTo(game.player1Id) shouldBe true
                    revealed?.isRevealedTo(game.player2Id) shouldBe true
                }
            }

            game.choose(decision, "Lightning Bolt")
            game.state.getEntity(game.alhammarret())?.chosenCardName() shouldBe "Lightning Bolt"

            withClue("the controller isn't an opponent, so their own Lightning Bolt can still be cast") {
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            }
            game.resolveStack()
            game.passPriority()
            withClue("an opponent can't cast a spell with the chosen name") {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
            }
        }

        test("a hand of only lands is still revealed, but there is no name to choose") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Alhammarret, High Arbiter")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardInHand(2, "Forest")
                .withCardInHand(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Alhammarret, High Arbiter").error shouldBe null
            game.resolveStack()
            val decision = game.pendingChoice()
            decision.prompt shouldBe "No nonland card was revealed"
            decision.options shouldContainExactly listOf("Continue")
            val forest = game.findCardsInHand(2, "Forest").first()
            game.state.getEntity(forest)?.get<RevealedToComponent>()?.isRevealedTo(game.player1Id) shouldBe true

            game.choose(decision, "Continue")
            withClue("Alhammarret enters with no name chosen") {
                game.state.getEntity(game.alhammarret())?.chosenCardName() shouldBe null
            }
            game.state.pendingDecision shouldBe null

        }

        test("with no name chosen, an opponent's spells are not stopped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Alhammarret, High Arbiter")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state.getEntity(game.alhammarret())?.chosenCardName() shouldBe null
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
        }

        test("an empty opposing hand reveals nothing and asks nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Alhammarret, High Arbiter")
                .withLandsOnBattlefield(1, "Island", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Alhammarret, High Arbiter").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.state.getEntity(game.alhammarret())?.chosenCardName() shouldBe null
        }

        test("returned to the battlefield by an effect, it still names a card from the revealed hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Alhammarret, High Arbiter")
                .withCardInHand(1, "Zombify")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingGraveyardCard(1, "Zombify", 1, "Alhammarret, High Arbiter").error shouldBe null
            game.resolveStack()
            val decision = game.pendingChoice()
            decision.options shouldContainExactly listOf("Grizzly Bears")
            game.choose(decision, "Grizzly Bears")
            game.state.getEntity(game.alhammarret())?.chosenCardName() shouldBe "Grizzly Bears"
        }

        test("the ban stops a face-up cast but not a face-down morph cast, and ends when Alhammarret leaves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Alhammarret, High Arbiter")
                .withCardInHand(2, "Dawning Purist")
                .withCardInHand(2, "Dawning Purist")
                .withCardInHand(2, "Swords to Plowshares")
                .withLandsOnBattlefield(2, "Plains", 8)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val alhammarret = game.alhammarret()
            game.state = game.state.updateEntity(alhammarret) {
                it.withCastChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice("Dawning Purist"))
            }

            game.castSpell(2, "Dawning Purist").error shouldNotBe null
            val purist = game.findCardsInHand(2, "Dawning Purist").first()
            withClue("a face-down spell has no name, so the ban doesn't see it") {
                game.execute(CastSpell(game.player2Id, purist, castFaceDown = true)).error shouldBe null
            }
            game.resolveStack()

            game.castSpell(2, "Swords to Plowshares", alhammarret).error shouldBe null
            game.resolveStack()
            game.findPermanent("Alhammarret, High Arbiter") shouldBe null
            withClue("with Alhammarret gone the name is no longer banned") {
                game.castSpell(2, "Dawning Purist").error shouldBe null
            }
        }
    }
}
