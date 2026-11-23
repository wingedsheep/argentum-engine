package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Frontline Medic — "Sacrifice this creature: Counter target spell with {X} in its mana cost unless
 * its controller pays {3}."
 *
 * The target restriction is the card-specific part: a spell whose printed cost has {X} (Mind Spring)
 * is a legal target and gets countered when its controller can't pay, while a spell without {X}
 * (Lightning Bolt) can't be targeted at all, so the activation is rejected and the Medic survives.
 */
class FrontlineMedicScenarioTest : ScenarioTestBase() {
    init {
        val sacId = cardRegistry.getCard("Frontline Medic")!!.script.activatedAbilities.single().id

        test("counters a spell with {X} in its mana cost when its controller can't pay {3}") {
            val game = scenario()
                .withPlayers("Caster", "Medic")
                .withCardInHand(1, "Mind Spring") // {X}{U}{U}
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardOnBattlefield(2, "Frontline Medic")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Mind Spring", xValue = 1).error shouldBe null
            val spring = game.state.getTopOfStack()!!
            game.passPriority() // priority to the Medic's controller

            val medic = game.findPermanent("Frontline Medic")!!
            val activation = game.execute(
                ActivateAbility(game.player2Id, medic, sacId, targets = listOf(ChosenTarget.Spell(spring)))
            )
            withClue("an {X} spell is a legal target: ${activation.error}") { activation.error shouldBe null }
            game.isInGraveyard(2, "Frontline Medic") shouldBe true

            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(false)

            withClue("Mind Spring is countered, not resolved") {
                game.isInGraveyard(1, "Mind Spring") shouldBe true
                game.state.stack.isEmpty() shouldBe true
            }
        }

        test("a spell without {X} in its mana cost can't be targeted") {
            val game = scenario()
                .withPlayers("Caster", "Medic")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Frontline Medic")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            val bolt = game.state.getTopOfStack()!!
            game.passPriority()

            val medic = game.findPermanent("Frontline Medic")!!
            val activation = game.execute(
                ActivateAbility(game.player2Id, medic, sacId, targets = listOf(ChosenTarget.Spell(bolt)))
            )
            withClue("Lightning Bolt has no {X}, so it isn't a legal target") { activation.error shouldNotBe null }
            game.isOnBattlefield("Frontline Medic") shouldBe true
        }
    }
}
