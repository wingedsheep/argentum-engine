package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.big.cards.EsotericDuplicator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Esoteric Duplicator (BIG #5) — {2}{U} Artifact — Clue.
 *
 * "Whenever you sacrifice this artifact or another artifact, you may pay {2}. If you do, at the
 *  beginning of the next end step, create a token that's a copy of that artifact.
 *  {2}, Sacrifice this artifact: Draw a card."
 *
 * Covers: the trigger fires when ANOTHER artifact is sacrificed and when the Duplicator sacrifices
 * ITSELF; paying {2} schedules a delayed end-step token copy of the just-sacrificed artifact (via
 * last-known information); declining the "may pay" makes no token.
 */
class EsotericDuplicatorScenarioTest : FunSpec({

    val clueDrawAbilityId = EsotericDuplicator.activatedAbilities.first().id

    fun tokenCopiesOf(driver: GameTestDriver, player: EntityId, cardName: String): List<EntityId> =
        driver.state.getBattlefield(player).filter { id ->
            val e = driver.state.getEntity(id) ?: return@filter false
            e.has<TokenComponent>() &&
                e.get<CardComponent>()?.name == cardName
        }

    test("sacrificing ANOTHER artifact, paying {2}, makes an end-step copy of it") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Esoteric Duplicator")
        // Another artifact (also a Clue) we will sacrifice via its own draw ability.
        val fodder = driver.putPermanentOnBattlefield(player, "Esoteric Duplicator")
        driver.giveMana(player, Color.BLUE, 4)

        // Sacrifice the fodder to its own "{2}, Sacrifice: Draw a card" ability.
        driver.submit(
            ActivateAbility(playerId = player, sourceId = fodder, abilityId = clueDrawAbilityId)
        ).error shouldBe null
        driver.bothPass() // resolve the draw ability → fodder is sacrificed → trigger fires

        // The Duplicator's trigger now asks "you may pay {2}".
        val mayPay = driver.pendingDecision
        (mayPay is YesNoDecision) shouldBe true
        driver.submitYesNo(player, true)
        // Pay the {2} from the pool (auto), then both pass to finish.
        driver.bothPass()

        // No token yet — it's scheduled for the next end step.
        tokenCopiesOf(driver, player, "Esoteric Duplicator").size shouldBe 0

        // Advance to the end step; the delayed trigger fires and creates the copy.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        tokenCopiesOf(driver, player, "Esoteric Duplicator").size shouldBe 1
    }

    test("declining the 'may pay {2}' creates no token") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Esoteric Duplicator")
        val fodder = driver.putPermanentOnBattlefield(player, "Esoteric Duplicator")
        driver.giveMana(player, Color.BLUE, 4)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = fodder, abilityId = clueDrawAbilityId)
        ).error shouldBe null
        driver.bothPass()

        val mayPay = driver.pendingDecision
        (mayPay is YesNoDecision) shouldBe true
        driver.submitYesNo(player, false)
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        tokenCopiesOf(driver, player, "Esoteric Duplicator").size shouldBe 0
    }

    test("the trigger fires when the Duplicator sacrifices ITSELF (this artifact)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dup = driver.putPermanentOnBattlefield(player, "Esoteric Duplicator")
        driver.giveMana(player, Color.BLUE, 4)

        // Sacrifice the Duplicator itself to its own draw ability.
        driver.submit(
            ActivateAbility(playerId = player, sourceId = dup, abilityId = clueDrawAbilityId)
        ).error shouldBe null
        driver.bothPass() // resolve draw → it sacrifices itself → its own trigger fires (this artifact)

        // The self-sacrifice still offers the "may pay {2}".
        val mayPay = driver.pendingDecision
        (mayPay is YesNoDecision) shouldBe true
        driver.submitYesNo(player, true)
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // A token copy of the Duplicator is created from its last-known printed characteristics.
        tokenCopiesOf(driver, player, "Esoteric Duplicator").size shouldBe 1
    }
})
