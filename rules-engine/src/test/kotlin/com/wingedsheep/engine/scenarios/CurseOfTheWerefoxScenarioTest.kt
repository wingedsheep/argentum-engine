package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Curse of the Werefox (WOE #167) — {2}{G} Sorcery.
 *
 * "Create a Monster Role token attached to target creature you control. When you do, that creature
 *  fights up to one target creature you don't control."
 *
 * The fight is a reflexive trigger (CR 603.12): the fight's target is chosen after the Role is
 * created, so the +1/+1 from the Monster Role is already applied when damage is dealt. The card
 * snapshots the spell's own target into a pipeline collection before the reflexive runs, because
 * the reflexive resolution rebinds `ContextTarget` to the reflexive target.
 */
class CurseOfTheWerefoxScenarioTest : ScenarioTestBase() {

    init {
        test("the enchanted creature fights with the Monster Role bonus already applied") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Curse of the Werefox")
                .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2
                .withCardOnBattlefield(2, "Centaur Courser")   // 3/3
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Curse of the Werefox", bears).error shouldBe null
            game.resolveStack()

            withClue("the Monster Role landed before the reflexive trigger asks for its target") {
                game.findPermanent("Monster Role") shouldNotBe null
                game.state.projectedState.getPower(bears) shouldBe 3
            }

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(courser))
            game.resolveStack()

            withClue("a 3/3 Bears and a 3/3 Courser fought — both died") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Centaur Courser") shouldBe false
            }
        }

        test("\"up to one\" — declining the fight target leaves the Role behind and nothing fights") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Curse of the Werefox")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Curse of the Werefox", bears).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.skipTargets()
            game.resolveStack()

            withClue("no fight happened; the Role stays attached") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.findPermanent("Monster Role") shouldNotBe null
                game.state.projectedState.getPower(bears) shouldBe 3
            }
        }
    }
}
