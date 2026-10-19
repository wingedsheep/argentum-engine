package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kairi, the Swirling Sky (NEO #60) — "When Kairi dies, choose one — • Return any number of target
 * nonland permanents with total mana value 6 or less to their owners' hands. • Mill six cards, then
 * return up to two instant and/or sorcery cards from your graveyard to your hand."
 *
 * The bounce mode's targets are chosen as the modal trigger goes on the stack, and the total mana
 * value cap must hold there: Grizzly Bears (2) + Hill Giant (4) is legal, adding Serra Angel (5) is not.
 */
class KairiTheSwirlingSkyScenarioTest : ScenarioTestBase() {

    /** Alice's Kairi dies to her own Murder. */
    private fun killKairi(): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Kairi, the Swirling Sky")
            .withCardInHand(1, "Murder")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Serra Angel")
            .withCardInLibrary(1, "Lightning Bolt")
            .withCardInLibrary(1, "Shock")
            .withCardInLibrary(1, "Counterspell")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Murder", game.findPermanent("Kairi, the Swirling Sky")!!).error shouldBe null
        game.resolveStack()
        game.isInGraveyard(1, "Kairi, the Swirling Sky") shouldBe true
        return game
    }

    private fun TestGame.chooseMode(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        context("Kairi, the Swirling Sky") {

            test("bounces any number of nonland permanents whose total mana value is 6 or less") {
                val game = killKairi()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val angel = game.findPermanent("Serra Angel")!!

                game.chooseMode("Return any number")
                val targets = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the decision carries the total mana value cap") {
                    targets.targetRequirements.single().totalManaValueAtMost shouldBe 6
                }
                withClue("Bears + Hill Giant + Serra Angel is total mana value 11") {
                    game.selectTargets(listOf(bears, giant, angel)).error.shouldNotBeNull()
                }
                game.selectTargets(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                withClue("Bears and Hill Giant return to Bob's hand; Serra Angel stays") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isInHand(2, "Hill Giant") shouldBe true
                    game.findPermanent("Serra Angel") shouldBe angel
                }
            }

            test("mills six, then returns up to two instant and/or sorcery cards") {
                val game = killKairi()

                game.chooseMode("Mill six")
                game.resolveStack()

                val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the three milled instants and the Murder that killed Kairi are offered, the Islands are not") {
                    select.options.size shouldBe 4
                }
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                val counterspell = game.findCardsInGraveyard(1, "Counterspell").single()
                game.selectCards(listOf(bolt, counterspell)).error shouldBe null

                withClue("two instants return; Shock and the three milled Islands stay in the graveyard") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                    game.isInHand(1, "Counterspell") shouldBe true
                    game.isInGraveyard(1, "Shock") shouldBe true
                    game.findCardsInGraveyard(1, "Island").size shouldBe 3
                }
            }
        }
    }
}
