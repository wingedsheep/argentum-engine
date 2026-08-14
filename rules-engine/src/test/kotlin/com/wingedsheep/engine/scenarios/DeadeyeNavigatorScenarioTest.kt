package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.PairedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Deadeye Navigator — {4}{U}{U} 5/5 Spirit with soulbond (CR 702.95) and
 * "As long as Deadeye Navigator is paired with another creature, each of those creatures has
 * '{1}{U}: Exile this creature, then return it to the battlefield under your control.'"
 *
 * The interesting half is that the granted activated ability must show up on *both* the Navigator
 * and its partner, and must blink whichever half it was activated on — a granted ability's source is
 * the permanent that has it (CR 113.7), not the granter.
 */
class DeadeyeNavigatorScenarioTest : ScenarioTestBase() {

    init {
        context("Deadeye Navigator") {

            /** Can [player] activate an ability whose source is [permanent] right now? */
            fun canActivateOn(game: TestGame, player: Int, permanent: EntityId): Boolean =
                game.getLegalActions(player).any {
                    (it.action as? ActivateAbility)?.sourceId == permanent
                }

            test("the blink ability is granted to both halves of the pair, and to neither while unpaired") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Deadeye Navigator")
                    .withCardInHand(1, "Grizzly Bears")
                    // Two Forests to cast the {1}{G} Bears, two Islands for the granted {1}{U} blink.
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .build()

                val navigator = game.findPermanent("Deadeye Navigator")!!

                withClue("an unpaired Navigator grants the ability to nobody, itself included") {
                    canActivateOn(game, 1, navigator) shouldBe false
                }

                val cast = game.castSpell(1, "Grizzly Bears")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                withClue("soulbond's second ability offers the pairing") { game.hasPendingDecision() shouldBe true }
                game.answerYesNo(true)

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("'each of those creatures' — the ability is activatable on both halves") {
                    canActivateOn(game, 1, navigator) shouldBe true
                    canActivateOn(game, 1, bears) shouldBe true
                }
            }

            test("blinking the partner returns the partner, not the Navigator, and breaks the pair") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Deadeye Navigator")
                    .withCardInHand(1, "Grizzly Bears")
                    // Two Forests for the {1}{G} Bears, and four Islands so there is still {1}{U}
                    // spare *after* the blink — otherwise "the ability is gone" and "the mana is
                    // gone" would be indistinguishable in the final assertions.
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .build()

                val cast = game.castSpell(1, "Grizzly Bears")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.answerYesNo(true)

                val navigator = game.findPermanent("Deadeye Navigator")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val blink = game.getLegalActions(1)
                    .mapNotNull { it.action as? ActivateAbility }
                    .firstOrNull { it.sourceId == bears }
                withClue("the partner should be offered the granted blink") { (blink != null) shouldBe true }

                val activate = game.execute(blink!!)
                withClue("activating the granted blink should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                // Activation opens the mana-payment window (CR 605.3a) — auto-tap the Islands.
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                // Exiling the partner broke the pair the instant it left (CR 702.95e is a continuous
                // fact, not an SBA-timed action), so when it re-enters the Navigator is unpaired and
                // its soulbond offers the pairing again — the printed loop players build around. That
                // re-trigger is also the proof the blink resolved: an untouched battlefield would
                // produce no enters-the-battlefield trigger at all.
                withClue("the returning creature re-triggers the Navigator's soulbond") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                withClue("the partner returned and the Navigator never moved — Self bound to the receiver (CR 113.7)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Deadeye Navigator") shouldBe true
                    game.state.getEntity(navigator)?.get<EnteredThisTurnComponent>() shouldBe null
                    game.state.getEntity(bears)?.get<EnteredThisTurnComponent>() shouldNotBe null
                }
                withClue("the blink broke the pair (CR 702.95e), and the re-pair was declined") {
                    // Assert the pairing state itself, not just the absence of the ability — with
                    // spare Islands still untapped, "no ability" now genuinely means "not paired".
                    game.state.getEntity(navigator)?.get<PairedComponent>() shouldBe null
                    game.state.getEntity(bears)?.get<PairedComponent>() shouldBe null
                    canActivateOn(game, 1, navigator) shouldBe false
                    canActivateOn(game, 1, bears) shouldBe false
                }
            }
        }
    }
}
