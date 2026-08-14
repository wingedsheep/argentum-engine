package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Lightning Mauler — {1}{R} 2/1 Human Berserker with soulbond (CR 702.95) and
 * "As long as this creature is paired with another creature, both creatures have haste."
 *
 * Exercises the whole soulbond mechanic through the simplest payoff there is: both halves of a
 * `Scope.SoulbondPair` static, the pairing decision on either trigger, the declined "you may", and
 * the CR 702.95e break.
 */
class LightningMaulerScenarioTest : ScenarioTestBase() {

    init {
        context("Lightning Mauler") {

            test("soulbond on its own ETB pairs it with a chosen unpaired creature, giving both haste") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Lightning Mauler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("an unpaired creature has no haste to begin with") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                val cast = game.castSpell(1, "Lightning Mauler")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // The soulbond ETB trigger pauses on the "you may pair" selection.
                withClue("soulbond's first ability asks which unpaired creature to pair with") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(bears))

                val mauler = game.findPermanent("Lightning Mauler")!!
                withClue("both halves of the pair have haste (CR 702.95b)") {
                    game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }

            test("declining the pairing leaves both creatures unpaired and hasteless") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Lightning Mauler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .build()

                game.castSpell(1, "Lightning Mauler")
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                // "You may pair" — selecting nothing declines (the chooseUpTo(1) lower bound).
                game.skipSelection()

                val mauler = game.findPermanent("Lightning Mauler")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("an unpaired Mauler grants haste to nobody, itself included") {
                    game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }
            }

            test("with no other creature to pair with, the trigger asks nothing and grants nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Lightning Mauler")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .build()

                game.castSpell(1, "Lightning Mauler")
                game.resolveStack()

                withClue("CR 702.95a's 'if you control … another creature' — an empty candidate set prompts nothing") {
                    game.hasPendingDecision() shouldBe false
                }
                val mauler = game.findPermanent("Lightning Mauler")!!
                game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe false
            }

            test("soulbond's second ability pairs a creature that enters later") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Lightning Mauler")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()

                val mauler = game.findPermanent("Lightning Mauler")!!
                withClue("the Mauler starts unpaired, so no haste") {
                    game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe false
                }

                val cast = game.castSpell(1, "Grizzly Bears")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("'Whenever another creature you control enters … you may pair' is a yes/no") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the newly-entered creature is now paired with the Mauler") {
                    game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }

            test("the pair breaks when a half leaves the battlefield, and haste goes with it (CR 702.95e)") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Lightning Mauler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Doom Blade is a sorcery, so it has to be player 1's — nobody else has
                    // sorcery-speed priority during this main phase.
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Mauler")
                game.resolveStack()
                game.selectCards(listOf(bears))

                val mauler = game.findPermanent("Lightning Mauler")!!
                game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe true

                // Kill the partner: it leaves the battlefield, so the pair breaks.
                val kill = game.castSpell(1, "Doom Blade", targetId = bears)
                withClue("the removal should succeed: ${kill.error}") { kill.error shouldBe null }
                game.resolveStack()

                withClue("the partner is gone") { game.isOnBattlefield("Grizzly Bears") shouldBe false }
                withClue("an unpaired Mauler loses the haste its own static was granting") {
                    game.state.projectedState.hasKeyword(mauler, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
