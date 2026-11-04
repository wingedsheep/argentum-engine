package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cherished Hatchling (RIX #124) — {1}{G} Creature — Dinosaur 2/1.
 *
 * "When this creature dies, you may cast Dinosaur spells this turn as though they had flash, and
 *  whenever you cast a Dinosaur spell this turn, it gains 'When this creature enters, you may have
 *  it fight another target creature.'"
 *
 * Pins the flash window (Dinosaurs only, only after the Hatchling dies), that the fight ability
 * granted to the *spell* survives onto the permanent and targets another creature, and the
 * resolution-time "you may" after the mandatory target is announced.
 */
class CherishedHatchlingScenarioTest : ScenarioTestBase() {

    private val dinosaur = card("Hatchling Test Dinosaur") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Dinosaur"
        power = 3
        toughness = 3
    }
    private val bear = card("Hatchling Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(dinosaur)
        cardRegistry.register(bear)

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Cherished Hatchling")
            .withCardInHand(1, "Doom Blade")
            .withCardInHand(1, "Hatchling Test Dinosaur")
            .withCardInHand(1, "Hatchling Test Bear")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        /** Leaves the granted ETB trigger waiting on its mandatory target decision. */
        fun TestGame.killHatchlingThenFlashDinosaurAtEndStep() {
            castSpell(1, "Doom Blade", findPermanent("Cherished Hatchling")!!).error shouldBe null
            resolveStack()
            isInGraveyard(1, "Cherished Hatchling") shouldBe true

            passUntilPhase(Phase.ENDING, Step.END)
            withClue("a non-Dinosaur spell gets no flash") {
                castSpell(1, "Hatchling Test Bear").error shouldNotBe null
            }
            castSpell(1, "Hatchling Test Dinosaur").error shouldBe null
            resolveStack() // cast trigger grants the ability, the Dinosaur resolves, its ETB triggers

            val dinoId = findPermanent("Hatchling Test Dinosaur")
            dinoId shouldNotBe null
            val targets = state.pendingDecision
            targets.shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("'another target creature' excludes the Dinosaur itself") {
                targets.legalTargets[0]!! shouldNotContain dinoId!!
                targets.legalTargets[0]!! shouldContain findPermanent("Grizzly Bears")!!
            }
        }

        test("after it dies, a Dinosaur can be cast at instant speed and fights on entering") {
            val game = builder().build()
            game.killHatchlingThenFlashDinosaurAtEndStep()

            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hatchling Test Dinosaur") shouldBe true
        }

        test("declining the fight leaves both creatures untouched") {
            val game = builder().build()
            game.killHatchlingThenFlashDinosaurAtEndStep()

            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null

            game.state.pendingDecision shouldBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hatchling Test Dinosaur") shouldBe true
        }

        test("while the Hatchling lives, Dinosaurs have no flash") {
            val game = builder().build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("no flash before the Hatchling dies") {
                game.castSpell(1, "Hatchling Test Dinosaur").error shouldNotBe null
            }
        }

        test("a Dinosaur cast at sorcery speed later that turn also gains the fight ability") {
            val game = builder().build()
            game.castSpell(1, "Doom Blade", game.findPermanent("Cherished Hatchling")!!).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Hatchling Test Dinosaur").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("a Dinosaur cast without the Hatchling dying enters with no fight trigger") {
            val game = builder().build()

            game.castSpell(1, "Hatchling Test Dinosaur").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
