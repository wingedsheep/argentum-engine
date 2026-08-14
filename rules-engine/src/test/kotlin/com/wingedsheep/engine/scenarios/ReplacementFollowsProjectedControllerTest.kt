package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A `Player.You` replacement effect follows whoever *currently* controls the permanent.
 *
 * Control change is a layer-2 continuous effect (`EffectApplicator`'s `ChangeController`
 * branch writes `projectedValues[..].controllerId`), so a stolen permanent's base
 * `ControllerComponent` still names its original controller for as long as the theft lasts.
 * `ReplacementEffectProcessor.gatherReplacements` therefore has to resolve the source's
 * controller through the projection — the project-wide rule for any battlefield read of
 * controller, and the one main's `checkStaticDrawReplacement` already followed via
 * `projected.getBattlefieldControlledBy`.
 *
 * Reading the base component instead is silent and inverted rather than merely absent: the
 * effect keeps firing for the player who lost the permanent and never fires for the one who
 * gained it. Both halves are asserted, because a fix that only stops the first would leave
 * Quantum Riddler doing nothing at all under Control Magic.
 *
 * Asserted through `gatherReplacements` rather than a full draw so the two directions can be
 * read off one board state; `ReplacementContextRegressionTest` covers the end-to-end draw.
 */
class ReplacementFollowsProjectedControllerTest : FunSpec({

    test("a stolen Quantum Riddler modifies its new controller's draws, not its owner's") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)

        val owner = driver.player1
        val thief = driver.getOpponent(owner)

        val riddler = driver.putPermanentOnBattlefield(owner, "Quantum Riddler")

        // Advance to the thief's precombat main so they can cast a sorcery-speed aura.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("Setup: the thief should be the active player") {
            (driver.state.activePlayerId == thief) shouldBe true
        }

        val processor = ReplacementEffectProcessor()

        // Quantum Riddler is gated on CardsInHandAtMost(1), and the gate is evaluated against
        // the drawing player — so both hands have to be empty for either direction to be
        // visible at all. Re-emptied after each step, since drawing refills them.
        fun emptyBothHands() {
            var s = driver.state
            for (player in listOf(owner, thief)) {
                for (card in driver.getHand(player)) {
                    s = s.removeFromZone(ZoneKey(player, Zone.HAND), card)
                        .addToZone(ZoneKey(player, Zone.GRAVEYARD), card)
                }
            }
            driver.replaceState(s)
        }

        fun appliesTo(player: EntityId): Boolean {
            emptyBothHands()
            return processor.gatherReplacements(
                driver.state,
                PendingGameEvent.DrawAmountPending(playerId = player, totalCount = 1)
            ).isNotEmpty()
        }

        withClue("Control: before the theft the Riddler modifies its owner's draws") {
            appliesTo(owner) shouldBe true
            appliesTo(thief) shouldBe false
        }

        driver.giveMana(thief, Color.BLUE, 4)
        val controlMagic = driver.putCardInHand(thief, "Control Magic")
        val cast = driver.castSpell(thief, controlMagic, targets = listOf(riddler))
        withClue("Control Magic should resolve: ${cast.error}") { cast.error shouldBe null }
        driver.bothPass()

        withClue("Setup: the theft is a projection-only change, so the base component is stale") {
            driver.state.projectedState.getController(riddler) shouldBe thief
            driver.state.getEntity(riddler)?.get<ControllerComponent>()?.playerId shouldBe owner
        }

        withClue("The ability moved with the permanent, so it must stop helping its owner") {
            appliesTo(owner) shouldBe false
        }
        withClue("...and must start helping whoever controls it now") {
            appliesTo(thief) shouldBe true
        }
    }
})
