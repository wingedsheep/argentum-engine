package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.assertions.withClue

/**
 * Tomik, Wielder of Law (MKM) — "Affinity for planeswalkers. Flying, vigilance. Whenever an
 * opponent attacks with creatures, if two or more of those creatures are attacking you and/or
 * planeswalkers you control, that opponent loses 3 life and you draw a card."
 *
 * Three things are worth pinning, and each is a place the engine previously said no:
 *
 *  - **Attackers pointed at your planeswalkers count.** `CreaturesAttackYouEvent` implements
 *    CR 509.1b the narrow way by default (Orim's Prayer), so Tomik has to opt in via
 *    `includePlaneswalkersYouControl`. The planeswalker-only attack is the test that would fail
 *    without it.
 *  - **"That opponent" resolves.** `AttackersDeclaredEvent` published an empty `TriggerContext`
 *    before this card, so `Player.TriggeringPlayer` evaluated to null and the life loss went
 *    nowhere.
 *  - **The intervening "if" is re-checked on resolution (CR 603.4).** Killing one of the two
 *    attackers in response has to make the whole ability do nothing — `minAttackers = 2` alone
 *    only checks at declaration.
 *
 * Affinity needed no engine change (`KeywordAbility.Affinity` is parameterized by card type), but
 * it's covered here because nothing else in the corpus exercises the planeswalker variant.
 */
class TomikWielderOfLawScenarioTest : ScenarioTestBase() {

    init {
        test("two attackers at you — that opponent loses 3 life and you draw") {
            val game = scenario()
                .withPlayers("Tomik", "Attacker")
                .withCardOnBattlefield(1, "Tomik, Wielder of Law", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val handBefore = game.handSize(1)

            game.declareAttackers(mapOf("Grizzly Bears" to 1, "Centaur Courser" to 1))
                .error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.handSize(1) shouldBe handBefore + 1
        }

        test("attackers split between you and a planeswalker you control still count — CR 509.1b widened") {
            val game = scenario()
                .withPlayers("Tomik", "Attacker")
                .withCardOnBattlefield(1, "Tomik, Wielder of Law", summoningSickness = false)
                .withCardOnBattlefield(1, "Chandra, Flameshaper")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val handBefore = game.handSize(1)

            game.declareAttackersWithPermanentTargets(
                playerAttackers = mapOf("Grizzly Bears" to 1),
                permanentAttackers = mapOf("Centaur Courser" to "Chandra, Flameshaper"),
            ).error shouldBe null
            game.resolveStack()

            withClue("one attacker at you plus one at your planeswalker is two of 'those creatures'") {
                game.getLifeTotal(2) shouldBe 17
                game.handSize(1) shouldBe handBefore + 1
            }
        }

        test("both attackers at a planeswalker you control — still two, still triggers") {
            val game = scenario()
                .withPlayers("Tomik", "Attacker")
                .withCardOnBattlefield(1, "Tomik, Wielder of Law", summoningSickness = false)
                .withCardOnBattlefield(1, "Chandra, Flameshaper")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf(
                    "Grizzly Bears" to "Chandra, Flameshaper",
                    "Centaur Courser" to "Chandra, Flameshaper",
                ),
            ).error shouldBe null
            game.resolveStack()

            withClue("this is exactly the case Orim's Prayer's narrow scoping would miss") {
                game.getLifeTotal(2) shouldBe 17
            }
        }

        test("a single attacker does not trigger it") {
            val game = scenario()
                .withPlayers("Tomik", "Attacker")
                .withCardOnBattlefield(1, "Tomik, Wielder of Law", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val handBefore = game.handSize(1)

            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.handSize(1) shouldBe handBefore
        }

        test("affinity for planeswalkers reduces the cost by one per planeswalker you control") {
            val game = scenario()
                .withPlayers("Tomik", "Other")
                .withCardInHand(1, "Tomik, Wielder of Law")
                .withCardOnBattlefield(1, "Chandra, Flameshaper")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("{1}{W}{B} minus {1} for the one planeswalker is castable off two lands") {
                game.castSpell(1, "Tomik, Wielder of Law").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Tomik, Wielder of Law") shouldBe true
            }
        }

        test("CR 603.4 — killing an attacker in response makes the ability do nothing") {
            val game = scenario()
                .withPlayers("Tomik", "Attacker")
                .withCardOnBattlefield(1, "Tomik, Wielder of Law", summoningSickness = false)
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                .withCardInLibrary(1, "Llanowar Elves")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val handBefore = game.handSize(1)

            game.declareAttackers(mapOf("Grizzly Bears" to 1, "Centaur Courser" to 1))
                .error shouldBe null

            // The trigger is on the stack. The attacking player holds priority first; once they
            // pass, Tomik's controller responds by burning one of the two attackers.
            game.passPriority()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("only one of 'those creatures' is still attacking, so the intervening if fails") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
                // The Bolt itself left hand; the draw never happened.
                game.handSize(1) shouldBe handBefore - 1
            }
        }
    }
}
