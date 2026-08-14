package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Spider-Ham, Peter Porker (SPM #114, {1}{G}) — Legendary Creature — Spider Boar Hero, 2/2.
 *
 * "When Spider-Ham enters, create a Food token."
 * "Animal May-Ham — Other Spiders, Boars, Bats, Bears, Birds, Cats, Dogs, Frogs, Jackals,
 *  Lizards, Mice, Otters, Rabbits, Raccoons, Rats, Squirrels, Turtles, and Wolves you control
 *  get +1/+1."
 *
 * Verifies the ETB Food trigger and that the static anthem pumps a listed subtype (Bear), leaves
 * a non-listed subtype (Ogre) alone, and — because it reads "Other" — does not pump Spider-Ham
 * itself even though it is a Spider and a Boar.
 */
class SpiderHamPeterPorkerScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("ETB creates a Food; anthem pumps listed types but not others nor Spider-Ham itself") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Spider-Ham, Peter Porker")
                .withCardOnBattlefield(1, "Grizzly Bears") // Bear — listed
                .withCardOnBattlefield(1, "Gray Ogre")     // Ogre — not listed
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Spider-Ham, Peter Porker").error shouldBe null
            game.resolveStack()

            withClue("ETB created a Food token") {
                game.findPermanents("Food").size shouldBe 1
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            val ogre = game.findPermanent("Gray Ogre")!!
            val spiderHam = game.findPermanent("Spider-Ham, Peter Porker")!!

            withClue("Grizzly Bears (Bear) gets +1/+1: 2/2 -> 3/3") {
                projector.getProjectedPower(game.state, bears) shouldBe 3
                projector.getProjectedToughness(game.state, bears) shouldBe 3
            }
            withClue("Gray Ogre (Ogre) is not a listed type: stays 2/2") {
                projector.getProjectedPower(game.state, ogre) shouldBe 2
                projector.getProjectedToughness(game.state, ogre) shouldBe 2
            }
            withClue("Spider-Ham does not pump itself (\"Other …\"): stays 2/2") {
                projector.getProjectedPower(game.state, spiderHam) shouldBe 2
                projector.getProjectedToughness(game.state, spiderHam) shouldBe 2
            }
        }
    }
}
