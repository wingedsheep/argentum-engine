package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Stenn, Paranoid Partisan (DMU #221).
 *
 * "As Stenn enters, choose a card type other than creature or land. Spells you cast of the chosen
 *  type cost {1} less to cast. {1}{W}{U}: Exile Stenn. Return it to the battlefield under its
 *  owner's control at the beginning of the next end step."
 *
 * The as-enters choice is an `EntersWithChoice(MODE)` whose option ids are card-type names, read by
 * the cost static through `ofChosenCardTypeComponent(ChoiceSlot.MODE)` — a wiring that only shows
 * up when a spell is actually cast for less. The blink returns a new object, which must choose again.
 */
class StennParanoidPartisanScenarioTest : ScenarioTestBase() {

    private val stenn = "Stenn, Paranoid Partisan"

    private fun TestGame.chooseType(type: String) {
        val decision = getPendingDecision() as? ChooseOptionDecision
            ?: error("Expected Stenn's card-type choice, got ${getPendingDecision()}")
        withClue("creature and land are never offered") {
            decision.options shouldNotContain "Creature"
            decision.options shouldNotContain "Land"
        }
        decision.options shouldContain type
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(type))).error shouldBe null
    }

    private fun TestGame.castStennChoosing(type: String) {
        castSpell(1, stenn).error shouldBe null
        resolveStack()
        chooseType(type)
        resolveStack()
        isOnBattlefield(stenn) shouldBe true
    }

    private fun TestGame.chosenType(): String? {
        val id = findPermanent(stenn) ?: return null
        return (state.getEntity(id)?.get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.MODE)
            as? ChoiceValue.TextChoice)?.text
    }

    init {
        test("sorcery chosen: Divination costs {1}{U} and is castable off two Islands") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, stenn)
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
            val game = builder.build()

            game.castStennChoosing("Sorcery")
            game.chosenType() shouldBe "Sorcery"

            val handBefore = game.handSize(1)
            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()
            withClue("Divination resolved: -1 card cast, +2 drawn") { game.handSize(1) shouldBe handBefore + 1 }
        }

        test("instant chosen: a sorcery is not reduced and two Islands can't pay for Divination") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, stenn)
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
            val game = builder.build()

            game.castStennChoosing("Instant")

            (game.castSpell(1, "Divination").error != null) shouldBe true
            game.isInHand(1, "Divination") shouldBe true
        }

        test("the blink exiles Stenn and returns it at the next end step, choosing a type afresh") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, stenn)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
            repeat(5) { builder = builder.withCardInLibrary(2, "Island") }
            val game = builder.build()

            game.castStennChoosing("Sorcery")
            val stennId = game.findPermanent(stenn)!!
            val abilityId = cardRegistry.getCard(stenn)!!.activatedAbilities.first().id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = stennId, abilityId = abilityId))
                .error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Stenn is in exile until the end step") {
                game.isOnBattlefield(stenn) shouldBe false
                game.isInExile(1, stenn) shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            withClue("Stenn returns as a new object and asks for a card type again") {
                game.chooseType("Artifact")
            }
            game.resolveStack()

            game.isOnBattlefield(stenn) shouldBe true
            game.chosenType() shouldBe "Artifact"
        }
    }
}
