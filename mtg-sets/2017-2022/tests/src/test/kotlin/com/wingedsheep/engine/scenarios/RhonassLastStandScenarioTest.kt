package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.RhonassLastStand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Rhonas's Last Stand — "Create a 5/4 green Snake creature token. Lands you control don't untap
 * during your next untap step."
 *
 * The caster gets one 5/4 green Snake, and every land they control at their next untap step —
 * including one that entered after resolution (2017-07-14 ruling) — stays tapped through that step
 * only.
 */
class RhonassLastStandScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RhonassLastStand))
        initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("creates a 5/4 green Snake, then the caster's lands skip only their next untap step") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val myForest = d.putPermanentOnBattlefield(me, "Forest").also(d::tapPermanent)
        val theirForest = d.putPermanentOnBattlefield(opp, "Forest").also(d::tapPermanent)

        val spell = d.putCardInHand(me, "Rhonas's Last Stand")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass().error shouldBe null

        val tokens = d.getCreatures(me).filter { d.state.getEntity(it)!!.has<TokenComponent>() }
        tokens shouldHaveSize 1
        val snake = tokens.single()
        val card = d.state.getEntity(snake)!!.get<CardComponent>()!!
        withClue("the token is a 5/4 green Snake") {
            d.state.projectedState.getPower(snake) shouldBe 5
            d.state.projectedState.getToughness(snake) shouldBe 4
            card.colors shouldBe setOf(Color.GREEN)
            card.typeLine.subtypes.map { it.value } shouldBe listOf("Snake")
        }
        d.getCreatures(opp).size shouldBe 0

        val lateForest = d.putPermanentOnBattlefield(me, "Forest").also(d::tapPermanent)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("the opponent untaps normally") {
            d.isTapped(theirForest) shouldBe false
            d.isTapped(myForest) shouldBe true
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the caster's next untap step leaves every land they control tapped") {
            d.isTapped(myForest) shouldBe true
            d.isTapped(lateForest) shouldBe true
            d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe false
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        withClue("the following untap step untaps them") {
            d.isTapped(myForest) shouldBe false
            d.isTapped(lateForest) shouldBe false
        }
    }
})
