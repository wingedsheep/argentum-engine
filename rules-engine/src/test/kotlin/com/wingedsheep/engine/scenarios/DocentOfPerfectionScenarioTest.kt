package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Docent of Perfection // Final Iteration (EMN) — Wizard tokens, the three-Wizard flip, and the
 * back face's two-row anthem.
 *
 * The flip condition is checked *after* the token is created, so the third cast is the one that
 * transforms it (the token it just made is the third Wizard), and only while the ability resolves.
 */
class DocentOfPerfectionScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun boltOpponent(driver: GameTestDriver, player: EntityId) {
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpellWithTargets(
            player, bolt, listOf(ChosenTarget.Player(driver.getOpponent(player)))
        )
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun wizardTokens(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getCreatures(player).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Human Wizard Token"
        }

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    test("each instant or sorcery makes a 1/1 blue Human Wizard token") {
        val (driver, you) = newGame()
        val docent = driver.putCreatureOnBattlefield(you, "Docent of Perfection")

        boltOpponent(driver, you)

        val tokens = wizardTokens(driver, you)
        tokens.size shouldBe 1
        val projected = projector.project(driver.state)
        projected.getPower(tokens.single()) shouldBe 1
        projected.getToughness(tokens.single()) shouldBe 1
        faceName(driver, docent) shouldBe "Docent of Perfection"
    }

    test("two Wizards is not enough to flip it") {
        val (driver, you) = newGame()
        val docent = driver.putCreatureOnBattlefield(you, "Docent of Perfection")

        repeat(2) { boltOpponent(driver, you) }

        wizardTokens(driver, you).size shouldBe 2
        faceName(driver, docent) shouldBe "Docent of Perfection"
    }

    test("the third Wizard transforms it into Final Iteration, which anthems every Wizard") {
        val (driver, you) = newGame()
        val docent = driver.putCreatureOnBattlefield(you, "Docent of Perfection")

        repeat(3) { boltOpponent(driver, you) }

        wizardTokens(driver, you).size shouldBe 3
        faceName(driver, docent) shouldBe "Final Iteration"

        val projected = projector.project(driver.state)
        projected.getPower(docent) shouldBe 6
        projected.getToughness(docent) shouldBe 5
        projected.hasKeyword(docent, Keyword.FLYING) shouldBe true

        // "Wizards you control get +2/+1 and have flying" — both static rows, over every Wizard.
        wizardTokens(driver, you).forEach { token ->
            projected.getPower(token) shouldBe 3
            projected.getToughness(token) shouldBe 2
            projected.hasKeyword(token, Keyword.FLYING) shouldBe true
        }
    }

    test("the anthem reaches Wizards the Docent didn't make, and not the opponent's") {
        val (driver, you) = newGame()
        val opponent = driver.getOpponent(you)
        val docent = driver.putCreatureOnBattlefield(you, "Docent of Perfection")

        // A printed Wizard on each side. Bog Initiate is a 1/1 Human Wizard.
        val yourWizard = driver.putCreatureOnBattlefield(you, "Bog Initiate")
        val theirWizard = driver.putCreatureOnBattlefield(opponent, "Bog Initiate")

        // Your Bog Initiate already counts, so the second cast is the one that hits three Wizards.
        repeat(2) { boltOpponent(driver, you) }
        faceName(driver, docent) shouldBe "Final Iteration"

        val projected = projector.project(driver.state)
        projected.getPower(yourWizard) shouldBe 3
        projected.getToughness(yourWizard) shouldBe 2
        projected.hasKeyword(yourWizard, Keyword.FLYING) shouldBe true
        projected.getPower(theirWizard) shouldBe 1
        projected.getToughness(theirWizard) shouldBe 1
        projected.hasKeyword(theirWizard, Keyword.FLYING) shouldBe false
    }
})
