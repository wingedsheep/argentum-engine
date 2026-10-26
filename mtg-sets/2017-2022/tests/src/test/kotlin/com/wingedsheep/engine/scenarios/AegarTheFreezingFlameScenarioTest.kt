package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.AegarTheFreezingFlame
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aegar, the Freezing Flame: "Whenever a creature or planeswalker an opponent controls is dealt
 * excess damage, if a Giant, Wizard, or spell you controlled dealt damage to it this turn, draw a
 * card." The clause reads the damaged permanent's record of its damage sources — including the
 * hit that dealt the excess, even when that hit destroyed it — and any earlier qualifying hit this
 * turn counts even if a non-qualifying source deals the excess (the 2021-02-05 ruling).
 */
class AegarTheFreezingFlameScenarioTest : ScenarioTestBase() {

    private val walker = card("Aegar Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 2
    }

    private val apprentice = card("Aegar Test Apprentice") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Human Wizard"
        power = 3
        toughness = 3
    }

    init {
        cardRegistry.register(AegarTheFreezingFlame)
        cardRegistry.register(listOf(walker, apprentice))

        fun board(vararg opponents: String): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Aegar, the Freezing Flame")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            opponents.forEach { builder.withCardOnBattlefield(2, it) }
            return builder.build()
        }

        fun TestGame.attackAndBeBlocked(attacker: String, blocker: String) {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf(attacker to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            declareBlockers(mapOf(blocker to listOf(attacker))).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
        }

        context("Aegar, the Freezing Flame") {
            test("your spell dealing excess damage draws a card, though it destroyed the creature") {
                val game = board("Grizzly Bears")
                val handBefore = game.handSize(1)

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("Bolt left the hand, the trigger drew one") { game.handSize(1) shouldBe handBefore }
            }

            test("excess damage to an opponent's planeswalker counts too") {
                val game = board("Aegar Test Walker")
                val handBefore = game.handSize(1)

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Aegar Test Walker")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Aegar Test Walker") shouldBe true
                withClue("3 damage to a 2-loyalty planeswalker is 1 excess") { game.handSize(1) shouldBe handBefore }
            }

            test("exactly lethal damage from your spell is not excess") {
                val game = board("Grizzly Bears")
                val handBefore = game.handSize(1)

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe handBefore - 1
            }

            test("excess combat damage from a creature that isn't a Giant or Wizard doesn't draw") {
                val game = board("Grizzly Bears")
                val handBefore = game.handSize(1)

                game.attackAndBeBlocked("Centaur Courser", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe handBefore
            }

            test("excess combat damage from a Giant you control draws a card") {
                val game = board("Grizzly Bears")
                val handBefore = game.handSize(1)

                game.attackAndBeBlocked("Aegar, the Freezing Flame", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe handBefore + 1
            }

            test("excess combat damage from a Wizard you control that isn't a Giant draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aegar, the Freezing Flame")
                    .withCardOnBattlefield(1, "Aegar Test Apprentice")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val handBefore = game.handSize(1)

                game.attackAndBeBlocked("Aegar Test Apprentice", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe handBefore + 1
            }

            test("an earlier hit from an opponent's spell doesn't count — it has to be yours") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aegar, the Freezing Flame")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val handBefore = game.handSize(1)

                game.castSpell(2, "Shock", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the Courser's 3 on top of the opponent's Shock is excess, but neither source qualifies") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("an earlier hit from your spell counts when another source deals the excess later") {
                val game = board("Hill Giant")
                val handBefore = game.handSize(1)

                game.castSpell(1, "Shock", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()
                withClue("2 damage to a 3/3 is not excess") { game.handSize(1) shouldBe handBefore - 1 }

                game.attackAndBeBlocked("Centaur Courser", "Hill Giant")

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                withClue("the Courser's 3 on top of the Shock's 2 is excess, and the Shock was your spell") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
