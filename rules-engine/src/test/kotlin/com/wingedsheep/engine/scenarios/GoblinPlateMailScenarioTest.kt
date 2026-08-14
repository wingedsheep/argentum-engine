package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goblin Plate Mail (HOB #157) — {1}{B/R} Artifact — Equipment.
 *
 * "When this Equipment enters, amass Goblins 1, then attach this Equipment to the amassed Army.
 *  Equipped creature gets +1/+0 and has menace.
 *  Equip {4}"
 *
 * The load-bearing part is "the amassed Army": the attach step is not a target, it reads the
 * Army the Amass step just chose out of the resolution pipeline
 * (`EntityReference.AmassedArmy.STORAGE_KEY`). Covered both when Amass has to create the Army
 * from nothing and when a pre-existing Army is grown instead.
 */
class GoblinPlateMailScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Plate Mail") {

            fun ScenarioTestBase.TestGame.armies() =
                state.getBattlefield().filter {
                    state.projectedState.isCreature(it) && state.projectedState.hasSubtype(it, "Army")
                }

            fun ScenarioTestBase.TestGame.castPlateMail() {
                castSpell(1, "Goblin Plate Mail")
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
            }

            test("entering amasses a Goblin Army and attaches itself to it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Goblin Plate Mail")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castPlateMail()

                val mail = game.findPermanent("Goblin Plate Mail")!!
                val army = game.armies().single()
                val projected = game.state.projectedState

                withClue("the amassed token is a Goblin Army") {
                    projected.hasSubtype(army, "Goblin") shouldBe true
                }
                withClue("0/0 base + one +1/+1 counter, then +1/+0 from the Equipment") {
                    projected.getPower(army) shouldBe 2
                    projected.getToughness(army) shouldBe 1
                }
                withClue("the Equipment attached itself to the Army it just amassed") {
                    game.state.getEntity(mail)?.get<AttachedToComponent>()?.targetId shouldBe army
                    projected.hasKeyword(army, Keyword.MENACE) shouldBe true
                }
            }

            test("a second copy grows the same Army instead of making a new one, and attaches there too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardsInHand(1, "Goblin Plate Mail", 2)
                    .withLandsOnBattlefield(1, "Mountain", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castPlateMail()
                val firstArmy = game.armies().single()

                game.castPlateMail()

                withClue("Amass grows the Army you already control — no second Army token") {
                    game.armies() shouldBe listOf(firstArmy)
                }
                withClue("both Equipment ended up on that Army") {
                    game.findAllPermanents("Goblin Plate Mail").forEach { mail ->
                        game.state.getEntity(mail)?.get<AttachedToComponent>()?.targetId shouldBe firstArmy
                    }
                }
                withClue("0/0 + two counters = 2/2, plus +1/+0 from each of the two Equipment") {
                    game.state.projectedState.getPower(firstArmy) shouldBe 4
                    game.state.projectedState.getToughness(firstArmy) shouldBe 2
                }
            }

            test("the ETB attach goes to the Army, not to another creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Goblin Plate Mail")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castPlateMail()

                val bears = game.findPermanent("Grizzly Bears")!!
                val mail = game.findPermanent("Goblin Plate Mail")!!
                withClue("the Bears are untouched — the Equipment is on the amassed Army") {
                    game.state.getEntity(mail)?.get<AttachedToComponent>()?.targetId shouldBe game.armies().single()
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe false
                }
            }
        }
    }
}
