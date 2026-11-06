package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.GremlinInfestation
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Gremlin Infestation (AER #83) — {3}{R} Enchantment — Aura.
 *
 * "Enchant artifact
 *  At the beginning of your end step, this Aura deals 2 damage to enchanted artifact's controller.
 *  When enchanted artifact is put into a graveyard, create a 2/2 red Gremlin creature token."
 *
 * The end step is the Aura controller's, but the damage goes to the artifact's controller — found at
 * resolution through the gathered enchanted permanent. The token goes to the Aura's controller even
 * though the artifact (and the Aura, as a state-based action) has left the battlefield.
 */
class GremlinInfestationScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GremlinInfestation, Shatter))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("at the beginning of your end step, the enchanted artifact's controller takes 2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val artifact = driver.putPermanentOnBattlefield(opponent, "Legend Rule Waiver")
        val infestation = driver.putPermanentOnBattlefield(me, "Gremlin Infestation")
        attach(driver, infestation, artifact)

        driver.passPriorityUntil(Step.END)
        driver.resolveStack()

        withClue("the opponent controls the enchanted artifact, so they take the damage") {
            driver.getLifeTotal(opponent) shouldBe 18
            driver.getLifeTotal(me) shouldBe 20
        }
    }

    test("when the enchanted artifact is put into a graveyard, the Aura's controller gets a 2/2 red Gremlin") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val artifact = driver.putPermanentOnBattlefield(opponent, "Legend Rule Waiver")
        val infestation = driver.putPermanentOnBattlefield(me, "Gremlin Infestation")
        attach(driver, infestation, artifact)

        val shatter = driver.putCardInHand(me, "Shatter")
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, shatter, listOf(artifact))
        driver.resolveStack()

        driver.getGraveyardCardNames(opponent) shouldContain "Legend Rule Waiver"
        driver.getGraveyardCardNames(me) shouldContain "Gremlin Infestation"

        val gremlins = driver.getCreatures(me).filter { driver.getCardName(it)?.contains("Gremlin") == true }
        withClue("one Gremlin token for the Aura's controller") { gremlins.size shouldBe 1 }
        driver.state.projectedState.getPower(gremlins.single()) shouldBe 2
        driver.state.projectedState.getToughness(gremlins.single()) shouldBe 2
        driver.getCreatures(opponent).size shouldBe 0
    }
})
