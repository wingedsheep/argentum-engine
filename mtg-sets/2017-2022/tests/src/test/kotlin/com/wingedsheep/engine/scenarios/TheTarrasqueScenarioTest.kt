package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Tarrasque (AFR #207) — {6}{G}{G}{G} 10/10 Legendary Creature — Dinosaur.
 *
 * "The Tarrasque has haste and ward {10} as long as it was cast.
 *  Whenever The Tarrasque attacks, it fights target creature defending player controls."
 *
 * Covers the "as long as it was cast" static gate both ways — a cast Tarrasque attacks the turn it
 * arrives and its ward counters unpaid removal; one put onto the battlefield without being cast has
 * neither — and the mandatory attack-trigger fight.
 */
class TheTarrasqueScenarioTest : ScenarioTestBase() {

    init {
        test("cast from hand, it has haste and attacks at once, fighting a defending creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "The Tarrasque")
                .withLandsOnBattlefield(1, "Forest", 9)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "The Tarrasque").error shouldBe null
            game.resolveStack()
            val tarrasque = game.findPermanent("The Tarrasque")!!

            withClue("it was cast, so it has haste and ward") {
                game.state.projectedState.hasKeyword(tarrasque, Keyword.HASTE) shouldBe true
                game.state.projectedState.hasKeyword(tarrasque, Keyword.WARD) shouldBe true
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("The Tarrasque" to 2)).error shouldBe null
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            withClue("the fight killed Grizzly Bears and dealt 2 damage back") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.state.getEntity(tarrasque)?.get<DamageComponent>()?.amount shouldBe 2
            }
        }

        test("cast, its ward {10} counters an opponent's removal they can't pay for") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "The Tarrasque")
                .withLandsOnBattlefield(1, "Forest", 9)
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "The Tarrasque").error shouldBe null
            game.resolveStack()
            game.passPriority()

            game.castSpell(2, "Murder", game.findPermanent("The Tarrasque")!!).error shouldBe null
            game.resolveStack()

            withClue("ward countered Murder") {
                game.isOnBattlefield("The Tarrasque") shouldBe true
                game.isInGraveyard(2, "Murder") shouldBe true
            }
        }

        test("put onto the battlefield without being cast, it has neither haste nor ward") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "The Tarrasque", summoningSickness = true)
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(1)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tarrasque = game.findPermanent("The Tarrasque")!!
            withClue("it wasn't cast, so the static grants nothing") {
                game.state.projectedState.hasKeyword(tarrasque, Keyword.HASTE) shouldBe false
                game.state.projectedState.hasKeyword(tarrasque, Keyword.WARD) shouldBe false
            }

            game.castSpell(2, "Murder", tarrasque).error shouldBe null
            game.resolveStack()

            withClue("no ward trigger, so Murder destroys it") {
                game.isInGraveyard(1, "The Tarrasque") shouldBe true
            }
        }

        test("without haste it can't attack the turn it was put onto the battlefield") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "The Tarrasque", summoningSickness = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("The Tarrasque" to 2)).error shouldNotBe null
        }
    }
}
