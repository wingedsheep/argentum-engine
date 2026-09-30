package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lion Umbra (MH3 #160): "Enchant modified creature. Enchanted creature gets +3/+3 and has reach and
 * vigilance. Umbra armor."
 *
 * Umbra armor's own rules live in `UmbraArmorTest`; this pins the card's enchant restriction, which
 * reads CR 700.9's "Auras its controller controls".
 */
class LionUmbraScenarioTest : ScenarioTestBase() {

    init {
        context("Lion Umbra") {

            fun modify(game: TestGame, name: String) {
                val id = game.findPermanent(name)!!
                game.state = game.state.updateEntity(id) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
            }

            test("can only enchant a modified creature, then pumps it and grants reach and vigilance") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(1, "Lion Umbra")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Lion Umbra", courser).error shouldNotBe null

                modify(game, "Centaur Courser")
                game.castSpell(1, "Lion Umbra", courser).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Lion Umbra") shouldBe true
                val projected = game.state.projectedState
                projected.getPower(courser) shouldBe 7
                projected.getToughness(courser) shouldBe 7
                projected.hasKeyword(courser, Keyword.REACH) shouldBe true
                projected.hasKeyword(courser, Keyword.VIGILANCE) shouldBe true
            }

            test("umbra armor destroys Lion Umbra instead of the creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardAttachedTo(1, "Lion Umbra", "Centaur Courser")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Doom Blade", game.findPermanent("Centaur Courser")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.isInGraveyard(1, "Lion Umbra") shouldBe true
            }

            test("on your own creature it keeps the creature modified by itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardAttachedTo(1, "Lion Umbra", "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()

                game.isOnBattlefield("Lion Umbra") shouldBe true
            }

            test("on another player's unmodified creature it doesn't count as a modification and falls off") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardAttachedTo(1, "Lion Umbra", "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()

                game.isInGraveyard(1, "Lion Umbra") shouldBe true
            }

            test("on another player's creature with a counter it stays attached") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardAttachedTo(1, "Lion Umbra", "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                modify(game, "Centaur Courser")

                game.checkStateBasedActions()

                game.isOnBattlefield("Lion Umbra") shouldBe true
            }
        }
    }
}
