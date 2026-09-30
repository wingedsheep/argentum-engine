package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class KuroPitlordAiTest : FunSpec({
    test("negative stat modifiers still prefer opposing creatures") {
        val registry = CardRegistry().apply { register(TestCards.all) }
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        }
        val kuro = driver.putCreatureOnBattlefield(driver.player1, "Kuro, Pitlord")
        driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val enemy = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val action = GameSimulator(registry).getLegalActions(driver.state, driver.player1)
            .first { (it.action as? ActivateAbility)?.sourceId == kuro }
        TargetSelection.fillHeuristically(
            driver.state, action, driver.player1, true, IntentCatalog.of(registry)
        ).shouldBeInstanceOf<ActivateAbility>().targets shouldBe listOf(ChosenTarget.Permanent(enemy))
    }
})
