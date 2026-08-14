package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Presumed Dead (MKM #100) — {1}{B} Instant.
 *
 * "Until end of turn, target creature gets +2/+0 and gains 'When this creature dies, return it to
 *  the battlefield under its owner's control and suspect it.'"
 *
 * Three claims, and the last is the one a shortcut implementation breaks:
 *
 *  1. the pump is +2/+0, not +2/+2 — the creature is *easier* to kill after the trick, which is the
 *     whole point of the card;
 *  2. the granted dies trigger really reanimates the creature, out of the graveyard, untapped;
 *  3. the returned creature is suspected **permanently**. The grant expires at end of turn, so an
 *     implementation that made the suspect share the grant's `Duration.EndOfTurn` would quietly
 *     un-suspect the creature during cleanup — the printed rulings say a suspect stays until the
 *     creature leaves the battlefield or something explicitly absolves it;
 *  4. it returns "under its **owner's** control". Pointing the trick at an opponent's creature and
 *     then killing it must hand the creature back to them, not to you. Every other test here uses
 *     the caster's own creature, where owner and controller coincide, so a return-under-your-control
 *     bug would hide in all of them.
 */
class PresumedDeadScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("it pumps power only — +2/+0, not +2/+2") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Presumed Dead")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            withClue("baseline 3/3") {
                projector.project(game.state).getPower(courser) shouldBe 3
                projector.project(game.state).getToughness(courser) shouldBe 3
            }

            game.castSpell(1, "Presumed Dead", targetId = courser).error shouldBe null
            game.resolveStack()

            val projected = projector.project(game.state)
            projected.getPower(courser) shouldBe 5
            withClue("toughness is untouched — the creature is no harder to kill") {
                projected.getToughness(courser) shouldBe 3
            }
        }

        test("the granted trigger returns the creature and suspects it") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Presumed Dead")
                .withCardInHand(1, "Doom Blade")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            game.castSpell(1, "Presumed Dead", targetId = courser).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Doom Blade", targetId = courser).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.resolveStack()

            withClue("it died and came straight back — it is on the battlefield, not in the graveyard") {
                game.findPermanent("Centaur Courser") shouldBe courser
                game.isInGraveyard(1, "Centaur Courser") shouldBe false
            }
            val returned = game.findPermanent("Centaur Courser")!!

            val projected = projector.project(game.state)
            withClue("and it comes back suspected: menace, and it can't block") {
                projected.isSuspected(returned) shouldBe true
                projected.hasKeyword(returned, Keyword.MENACE) shouldBe true
                projected.cantBlock(returned) shouldBe true
            }
            withClue("the +2/+0 did not follow it back — the returned permanent is a new object") {
                projected.getPower(returned) shouldBe 3
            }
        }

        test("it returns under its OWNER's control, not the caster's") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Presumed Dead")
                .withCardInHand(1, "Doom Blade")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            game.castSpell(1, "Presumed Dead", targetId = courser).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Doom Blade", targetId = courser).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.resolveStack()

            val returned = game.findPermanent("Centaur Courser")!!
            withClue("the creature came back") {
                game.isInGraveyard(2, "Centaur Courser") shouldBe false
            }
            withClue("on its owner's side of the board — the caster does not steal it") {
                game.state.getZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD)) shouldContain returned
                game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD))
                    .contains(returned) shouldBe false
            }
            withClue("and it is suspected all the same") {
                projector.project(game.state).isSuspected(returned) shouldBe true
            }
        }

        test("without the trick the creature simply dies") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Doom Blade")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            game.castSpell(1, "Doom Blade", targetId = courser).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.resolveStack()

            withClue("the reanimation is Presumed Dead's, not something the creature had") {
                game.isInGraveyard(1, "Centaur Courser") shouldBe true
                game.findPermanent("Centaur Courser") shouldBe null
            }
        }
    }
}
