package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.CinderStrike
import com.wingedsheep.mtg.sets.definitions.ecl.cards.EvershrikesGift
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BlightPaymentAiTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CinderStrike, EvershrikesGift))
        initMirrorMatch(Deck.of("Mountain" to 20, "Plains" to 20))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("AI materializes the Blight target for an activated ability") {
        val driver = driver()
        val player = driver.activePlayer!!
        val gift = driver.putCardInGraveyard(player, "Evershrike's Gift")
        val creature = driver.putCreatureOnBattlefield(player, "Force of Nature")
        repeat(2) { driver.putLandOnBattlefield(player, "Plains") }
        val abilityId = EvershrikesGift.activatedAbilities.first { it.activateFromZone == Zone.GRAVEYARD }.id
        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { (it.action as? ActivateAbility)?.abilityId == abilityId }

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as ActivateAbility
        chosen.sourceId shouldBe gift
        chosen.costPayment?.blightTargets shouldBe listOf(creature)
        driver.submit(chosen).isSuccess shouldBe true
    }

    test("AI materializes the Blight branch instead of being charged its alternative mana") {
        val driver = driver()
        val player = driver.activePlayer!!
        val blightCreature = driver.putCreatureOnBattlefield(player, "Force of Nature")
        val opponent = driver.state.turnOrder.first { it != player }
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Cinder Strike")
        driver.putLandOnBattlefield(player, "Mountain")
        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { action ->
                (action.action as? CastSpell)?.cardId == spell && action.additionalCostInfo?.costType == "Blight"
            }

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as CastSpell
        chosen.additionalCostPayment?.blightTargets shouldBe listOf(blightCreature)
        driver.submit(chosen).isSuccess shouldBe true
    }
})
