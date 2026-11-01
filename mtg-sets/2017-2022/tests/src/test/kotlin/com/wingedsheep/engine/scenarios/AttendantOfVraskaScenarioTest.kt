package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Attendant of Vraska (GRN #271) — {1}{B}{G} 3/3 Zombie Soldier.
 *
 *   When this creature dies, if you control a Vraska planeswalker, you gain life equal to this
 *   creature's power.
 */
class AttendantOfVraskaScenarioTest : ScenarioTestBase() {

    init {
        val slay = card("Slay") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                val c = target(TargetFilter.Creature)
                effect = Effects.Destroy(c)
            }
        }
        cardRegistry.register(listOf(slay))

        test("dies while you control a Vraska planeswalker: gain life equal to its power") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Attendant of Vraska")
                .withCardOnBattlefield(1, "Vraska, Betrayal's Sting")
                .withCardInHand(1, "Slay")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val attendant = game.findPermanent("Attendant of Vraska")!!
            game.castSpell(1, "Slay", targetId = attendant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Attendant of Vraska") shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }

        test("dies without a Vraska planeswalker: no life gained") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Attendant of Vraska")
                .withCardInHand(1, "Slay")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val attendant = game.findPermanent("Attendant of Vraska")!!
            game.castSpell(1, "Slay", targetId = attendant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Attendant of Vraska") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
