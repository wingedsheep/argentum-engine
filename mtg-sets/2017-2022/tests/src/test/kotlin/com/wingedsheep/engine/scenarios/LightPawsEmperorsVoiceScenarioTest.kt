package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Light-Paws, Emperor's Voice (NEO #25) — "Whenever an Aura you control enters, if you cast it,
 * you may search your library for an Aura card with mana value less than or equal to that Aura
 * and with a different name than each Aura you control, put that card onto the battlefield
 * attached to Light-Paws, then shuffle."
 *
 * Pins the composition: the search pool is capped by the *triggering* Aura's mana value and
 * excludes names of Auras you control (the triggering one included); the found Aura enters
 * attached to Light-Paws; and because that Aura wasn't cast, it doesn't re-trigger Light-Paws.
 */
class LightPawsEmperorsVoiceScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Light-Paws, Emperor's Voice")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardInHand(1, "Holy Strength")
        .withCardInLibrary(1, "Holy Strength")
        .withCardInLibrary(1, "Unholy Strength")
        .withCardInLibrary(1, "Pacifism")
        .withCardInLibrary(1, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Light-Paws, Emperor's Voice") {

            test("a cast Aura fetches a cheaper-or-equal, differently named Aura onto Light-Paws, without re-triggering") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val unholy = game.findCardsInLibrary(1, "Unholy Strength").single()

                game.castSpell(1, "Holy Strength", bears).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Holy Strength shares a name with an Aura you control; Pacifism's mana value is 2 > 1") {
                    decision.options shouldBe listOf(unholy)
                }
                game.selectCards(listOf(unholy)).error shouldBe null
                game.resolveStack()

                val lightPaws = game.findPermanent("Light-Paws, Emperor's Voice")!!
                withClue("Unholy Strength entered attached to Light-Paws") {
                    val aura = game.findPermanent("Unholy Strength")!!
                    game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe lightPaws
                    game.state.projectedState.getPower(lightPaws) shouldBe 4
                    game.state.projectedState.getToughness(lightPaws) shouldBe 3
                }
                withClue("the fetched Aura wasn't cast, so Light-Paws doesn't trigger again") {
                    game.getPendingDecision() shouldBe null
                    game.state.stack.size shouldBe 0
                }
                game.librarySize(1) shouldBe 3
            }

            test("declining the search leaves the library untouched") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Holy Strength", bears).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Unholy Strength") shouldBe false
                game.librarySize(1) shouldBe 4
            }
        }
    }
}
