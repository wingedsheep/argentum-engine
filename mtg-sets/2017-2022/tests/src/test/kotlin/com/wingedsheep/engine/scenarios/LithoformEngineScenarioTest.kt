package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.LithoformEngine
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lithoform Engine (ZNR #245) — the third ability, "{4}, {T}: Copy target permanent spell you
 * control", copying a *noncreature* permanent spell: the copy resolves into a token artifact
 * alongside the original card. (Ability copies and instant/sorcery copies reuse the paths
 * Peter Parker's Camera and Choreographed Sparks already exercise.)
 */
class LithoformEngineScenarioTest : FunSpec({

    val trinket = card("LE Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LithoformEngine, trinket))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    val copyPermanentSpellId = LithoformEngine.activatedAbilities[2].id

    fun trinketsOnBattlefield(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId) =
        driver.getPermanents(player).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "LE Test Trinket"
        }

    test("copying an artifact spell puts a token copy onto the battlefield alongside the original") {
        val driver = setup()
        val me = driver.activePlayer!!

        val engine = driver.putPermanentOnBattlefield(me, "Lithoform Engine")
        val trinketCard = driver.putCardInHand(me, "LE Test Trinket")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, trinketCard).error shouldBe null
        val spellOnStack = driver.getTopOfStack()!!

        driver.giveColorlessMana(me, 4)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = engine, abilityId = copyPermanentSpellId,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )
        driver.isTapped(engine) shouldBe true

        var guard = 0
        while (driver.stackSize > 0 && guard < 20) { driver.bothPass(); guard++ }

        val trinkets = trinketsOnBattlefield(driver, me)
        trinkets.size shouldBe 2
        trinkets.count { driver.state.getEntity(it)!!.has<TokenComponent>() } shouldBe 1
        trinkets.contains(trinketCard) shouldBe true
    }
})
