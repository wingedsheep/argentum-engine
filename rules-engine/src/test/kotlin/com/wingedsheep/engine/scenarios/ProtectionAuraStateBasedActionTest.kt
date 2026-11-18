package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * CR 702.16c: a permanent with protection can't be enchanted by Auras that have the stated
 * quality — such Auras are put into their owners' graveyards as a state-based action. This
 * covers protection gained AFTER the Aura attached (targeting-time protection is a separate,
 * already-enforced check).
 *
 * The classic Alpha case: a creature wearing Holy Strength (a white Aura) gains protection
 * from white via White Ward → Holy Strength falls off into the graveyard; White Ward itself
 * stays ("This effect doesn't remove this Aura"), and off-color Auras are untouched.
 *
 * That sparing ([com.wingedsheep.sdk.scripting.ProtectionSpares]) is per effect, not per color:
 * the same protection from any source that doesn't spare the Aura — printed, granted later, or
 * another sparing grant that spares only *itself* — still removes it.
 */
class ProtectionAuraStateBasedActionTest : ScenarioTestBase() {

    init {
        context("protection gained after attachment removes same-color auras") {
            test("White Ward's pro-white sends an attached white aura to the graveyard, keeps itself and off-color auras") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")   // white aura
                    .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears") // black aura
                    .withCardInHand(1, "White Ward")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpell(1, "White Ward", bears)
                withClue("Casting White Ward on the (not yet protected) creature should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Holy Strength (white) becomes an illegal attachment and goes to the graveyard") {
                    game.isOnBattlefield("Holy Strength") shouldBe false
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                }
                withClue("White Ward itself stays attached — its own effect doesn't remove it") {
                    game.isOnBattlefield("White Ward") shouldBe true
                }
                withClue("Unholy Strength (black) is unaffected by protection from white") {
                    game.isOnBattlefield("Unholy Strength") shouldBe true
                }
                withClue("The enchanted creature is unharmed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("sparing is per effect, not per color") {
            test("printed protection from white still removes White Ward") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Black Knight") // protection from white
                    .withCardAttachedTo(1, "White Ward", "Black Knight")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions().error shouldBe null

                withClue("Black Knight's own protection from white doesn't spare White Ward") {
                    game.isInGraveyard(1, "White Ward") shouldBe true
                }
            }

            test("protection from white granted after White Ward removes it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Wizard")
                    .withCardAttachedTo(1, "White Ward", "Goblin Wizard")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions().error shouldBe null
                withClue("White Ward's own protection spares it") {
                    game.isOnBattlefield("White Ward") shouldBe true
                }

                val wizard = game.findPermanent("Goblin Wizard")!!
                // {R}: Target Goblin gains protection from white until end of turn.
                val abilityId = cardRegistry.getCard("Goblin Wizard")!!.activatedAbilities[1].id
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = wizard, abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(wizard)))
                )
                withClue("activation: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the later, ordinary protection from white removes White Ward") {
                    game.isInGraveyard(1, "White Ward") shouldBe true
                }
            }

            test("protection from white copied from a graveyard card removes White Ward") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cairn Wanderer")
                    .withCardAttachedTo(1, "White Ward", "Cairn Wanderer")
                    // A creature card with protection from white in a graveyard: Cairn Wanderer has it too.
                    .withCardInGraveyard(2, "Black Knight")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions().error shouldBe null

                withClue("Cairn Wanderer's ordinary protection from white doesn't spare White Ward") {
                    game.isInGraveyard(1, "White Ward") shouldBe true
                }
            }

            test("two self-sparing Auras each remove the other") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "White Ward", "Grizzly Bears")
                    // White, so it grants protection from white too — sparing only itself.
                    .withCardAttachedTo(1, "Pledge of Loyalty", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions().error shouldBe null

                withClue("Pledge of Loyalty's protection from white removes White Ward") {
                    game.isInGraveyard(1, "White Ward") shouldBe true
                }
                withClue("White Ward's protection from white removes Pledge of Loyalty") {
                    game.isInGraveyard(1, "Pledge of Loyalty") shouldBe true
                }
            }
        }
    }
}
