package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf

class SkrelvDefectorMiteScenarioTest : ScenarioTestBase() {

    init {
        context("Skrelv, Defector Mite") {
            test("deals poison counters with toxic combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Skrelv, Defector Mite")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val skrelv = game.findPermanent("Skrelv, Defector Mite")!!

                game.execute(DeclareAttackers(game.player1Id, mapOf(skrelv to game.player2Id))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(DeclareBlockers(game.player2Id, emptyMap())).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 19
                val counters = game.state.getEntity(game.player2Id)?.get<CountersComponent>()
                counters?.getCount(CounterType.POISON) shouldBe 1
            }

            test("cannot block") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(2, "Skrelv, Defector Mite")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hero = game.findPermanent("Devoted Hero")!!
                val skrelv = game.findPermanent("Skrelv, Defector Mite")!!

                game.execute(DeclareAttackers(game.player1Id, mapOf(hero to game.player2Id))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(DeclareBlockers(game.player2Id, mapOf(skrelv to listOf(hero))))
                blockResult.error shouldNotBe null
                blockResult.error!! shouldContainIgnoringCase "can't block"
            }

            test("activated ability grants toxic, hexproof, and color-specific evasion") {
                val game = scenario()
                    .withPlayers("Skrelv Player", "Defender")
                    .withCardOnBattlefield(1, "Skrelv, Defector Mite")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skrelv = game.findPermanent("Skrelv, Defector Mite")!!
                val hero = game.findPermanent("Devoted Hero")!!
                val goblin = game.findPermanent("Raging Goblin")!!
                val ability = cardRegistry.getCard("Skrelv, Defector Mite")!!.script.activatedAbilities[1]

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = skrelv,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(hero))
                    )
                )
                withClue("Skrelv's pay-life ability should activate: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.getLifeTotal(1) shouldBe 18

                game.resolveStack()
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(decision.id, Color.RED)).error shouldBe null

                val projected = StateProjector().project(game.state)
                projected.hasKeyword(hero, "TOXIC_1") shouldBe true
                projected.hasKeyword(hero, "HEXPROOF_FROM_RED") shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.execute(DeclareAttackers(game.player1Id, mapOf(hero to game.player2Id))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val redBlockResult = game.execute(DeclareBlockers(game.player2Id, mapOf(goblin to listOf(hero))))
                redBlockResult.error shouldNotBe null
                redBlockResult.error!! shouldContainIgnoringCase "can't be blocked by red creatures"

                game.execute(DeclareBlockers(game.player2Id, emptyMap())).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val counters = game.state.getEntity(game.player2Id)?.get<CountersComponent>()
                counters?.getCount(CounterType.POISON) shouldBe 1
            }
        }
    }
}
