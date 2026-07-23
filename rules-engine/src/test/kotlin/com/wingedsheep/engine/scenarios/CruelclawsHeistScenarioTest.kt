package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.CruelclawsHeist
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Cruelclaw's Heist (BLB) — {B}{B} Sorcery.
 *
 * "Gift a card … Target opponent reveals their hand. You choose a nonland card from it. Exile that
 *  card. If the gift was promised, you may cast that card for as long as it remains exiled, **and
 *  mana of any type can be spent to cast it**."
 *
 * Regression guard for the missing "mana of any type" clause (issue #1353): the gift mode granted a
 * permanent may-play-from-exile permission, but without `withAnyManaType`, so a stolen off-color
 * card could never actually be paid for with the heist controller's own (black) mana.
 */
class CruelclawsHeistScenarioTest : FunSpec({

    // A green creature in the opponent's hand. Its {G}{G} cost is the whole point: paying it from
    // Swamps is only possible because mana of any type can be spent to cast it.
    val greenCreature = card("Heist Test Bear") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CruelclawsHeist)
        driver.registerCard(greenCreature)
        // An all-Swamp mirror match: the only nonland card in the opponent's hand is the one we
        // plant there, so the "choose a nonland card" step is unambiguous.
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    test("gift mode exiles an off-color card that can then be cast with mana of any type") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val heist = driver.putCardInHand(me, "Cruelclaw's Heist")
        val stolenCard = driver.putCardInHand(opponent, "Heist Test Bear")
        // Four Swamps: {B}{B} for the heist, then two more black mana for the stolen {G}{G} card.
        repeat(4) { driver.putLandOnBattlefield(me, "Swamp") }

        // Mode index 1 = "Promise a gift" — the mode that grants the cast-from-exile permission.
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = heist,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(opponent))),
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )

        // Resolve the spell: the opponent draws their gift, reveals their hand, and I pick the
        // only nonland card in it.
        run {
            repeat(25) {
                if (driver.state.mayPlayPermissions.any { stolenCard in it.cardIds }) return@run
                if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            }
        }

        // The chosen card was exiled and I hold a permanent permission to cast it from exile.
        driver.state.getZone(opponent, Zone.EXILE).contains(stolenCard).shouldBeTrue()
        val permission = driver.state.mayPlayPermissions.single { stolenCard in it.cardIds }
        permission.controllerId shouldBe me
        permission.permanent shouldBe true
        // The regression: without this the {G}{G} cost can never be paid from my Swamps.
        permission.withAnyManaType shouldBe true

        // The exiled card shows up as castable for me...
        driver.castActionsFor(me, stolenCard).isNotEmpty().shouldBeTrue()

        // ...and actually resolves onto my battlefield, paid for with black mana.
        driver.castSpell(me, stolenCard).isSuccess shouldBe true
        repeat(6) {
            if (driver.state.getZone(me, Zone.BATTLEFIELD).contains(stolenCard)) return@repeat
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }

        driver.state.getZone(me, Zone.BATTLEFIELD).contains(stolenCard).shouldBeTrue()
        driver.state.getEntity(stolenCard)?.get<CardComponent>()?.name shouldBe "Heist Test Bear"
    }

    test("no-gift mode exiles the card without granting any cast permission") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val heist = driver.putCardInHand(me, "Cruelclaw's Heist")
        val stolenCard = driver.putCardInHand(opponent, "Heist Test Bear")
        repeat(4) { driver.putLandOnBattlefield(me, "Swamp") }

        // Mode index 0 = "Don't promise a gift" — exile only, no cast permission.
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = heist,
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(opponent))),
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )

        run {
            repeat(25) {
                if (driver.state.getZone(opponent, Zone.EXILE).contains(stolenCard)) return@run
                if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            }
        }

        driver.state.getZone(opponent, Zone.EXILE).contains(stolenCard).shouldBeTrue()
        driver.state.mayPlayPermissions.any { stolenCard in it.cardIds } shouldBe false
        driver.castActionsFor(me, stolenCard).isEmpty().shouldBeTrue()
    }
})
