package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.SanguineSavior
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sanguine Savior — {1}{W}{B} 2/1 Vampire Cleric with flying, lifelink, Disguise {W/B}{W/B}, and
 * "when this creature is turned face up, another target creature you control gains lifelink until
 * end of turn."
 *
 * The trigger is face-up-only, unlike its "enters **or** is turned face up" neighbours, so the two
 * lines diverge: hard-cast, it is just a flying lifelinker; disguised, flipping it also hands
 * lifelink to a second creature. CR 702.168d — turning face up is not entering the battlefield —
 * is what keeps the two apart.
 */
class SanguineSaviorScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SanguineSavior))
        return driver
    }

    test("hard-cast: a 2/1 flying lifelinker whose trigger never fires") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val card = driver.putCardInHand(player, "Sanguine Savior")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.BLACK, 2)

        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        val savior = driver.findPermanent(player, "Sanguine Savior")
        savior.shouldNotBeNull()
        val projected = driver.state.projectedState
        projected.getPower(savior) shouldBe 2
        projected.getToughness(savior) shouldBe 1
        projected.hasKeyword(savior, Keyword.FLYING) shouldBe true
        projected.hasKeyword(savior, Keyword.LIFELINK) shouldBe true
        // No face-up trigger on the entry route — the Bears gain nothing.
        projected.hasKeyword(bear, Keyword.LIFELINK) shouldBe false
    }

    test("the disguise line flips face up and hands lifelink to another creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val card = driver.putCardInHand(player, "Sanguine Savior")
        driver.giveColorlessMana(player, 3)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()

        val savior = driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }

        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.BLACK, 1)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = savior,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null

        // The face-up trigger asks for its target; only the Bears is legal ("another").
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(player, listOf(bear)).error shouldBe null
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(savior) shouldBe 2
        projected.hasKeyword(savior, Keyword.FLYING) shouldBe true
        projected.hasKeyword(bear, Keyword.LIFELINK) shouldBe true
    }
})
