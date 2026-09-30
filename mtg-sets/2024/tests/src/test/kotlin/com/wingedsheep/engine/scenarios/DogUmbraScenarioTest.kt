package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dog Umbra (MH3 #22): "Flash. Enchant creature. As long as another player controls enchanted
 * creature, it can't attack or block. Otherwise, this Aura has umbra armor."
 *
 * Umbra armor's own rules live in `UmbraArmorTest`; this pins the card's conditional split.
 */
class DogUmbraScenarioTest : ScenarioTestBase() {

    init {
        context("Dog Umbra") {

            test("cast on your own creature, it has umbra armor and saves it from destruction") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(1, "Dog Umbra")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Dog Umbra", courser).error shouldBe null
                game.resolveStack()
                val umbra = game.findPermanent("Dog Umbra")!!
                game.state.projectedState.hasKeyword(umbra, Keyword.UMBRA_ARMOR) shouldBe true

                game.castSpell(1, "Doom Blade", courser).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.isInGraveyard(1, "Dog Umbra") shouldBe true
            }

            test("on another player's creature, it can't attack and has no umbra armor") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardAttachedTo(1, "Dog Umbra", "Centaur Courser")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val umbra = game.findPermanent("Dog Umbra")!!
                game.state.projectedState.hasKeyword(umbra, Keyword.UMBRA_ARMOR) shouldBe false

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldNotBe null
            }

            test("on another player's creature, destruction isn't replaced") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardAttachedTo(1, "Dog Umbra", "Centaur Courser")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Doom Blade", game.findPermanent("Centaur Courser")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Centaur Courser") shouldBe true
            }
        }
    }
}
