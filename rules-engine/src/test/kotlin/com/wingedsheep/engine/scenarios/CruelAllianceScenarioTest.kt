package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Cruel Alliance (MSH #92) — {2}{B} Sorcery.
 *
 *   Teamwork 2
 *   Exile target creature with mana value 3 or less. If this spell was cast using teamwork,
 *   instead exile target creature and you gain 3 life.
 *
 * The interesting half is the *targeting*: "instead" replaces the target restriction, so the
 * teamwork cast can hit a creature the plain cast could not even announce. Grizzly Bears
 * ({1}{G}, mana value 2) is the plain victim and Craw Wurm ({4}{G}{G}, mana value 6) is the one
 * only teamwork reaches.
 *
 * The last two tests go against `getLegalActions` rather than `execute(CastSpell(...))` — the
 * handler can't tell you whether the client was *offered* the branch, and every teamwork bug found
 * so far has lived in the enumerator.
 */
class CruelAllianceScenarioTest : ScenarioTestBase() {

    init {
        context("Cruel Alliance") {

            test("cast without teamwork exiles a mana value 3 or less creature and gains no life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cruel Alliance")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(1, "Cruel Alliance", targetId = bears).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                withClue("the life gain rides only on the teamwork branch") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("no teamwork cost was declared, so nothing tapped") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
            }

            test("the plain cast cannot target a creature with mana value 4 or greater") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cruel Alliance")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()

                game.castSpell(1, "Cruel Alliance", targetId = wurm).error.shouldNotBeNull()
                game.isInHand(1, "Cruel Alliance") shouldBe true
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }

            test("cast using teamwork exiles any creature and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cruel Alliance")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                // Teamwork 2 — the 3/3 Hill Giant clears the threshold on its own.
                game.castSpellWithTeamwork(
                    1, "Cruel Alliance", "Hill Giant", targetId = wurm,
                ).error shouldBe null
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                withClue("the teamwork branch has no mana value restriction") {
                    game.isInExile(2, "Craw Wurm") shouldBe true
                }
                game.getLifeTotal(1) shouldBe 23
            }

            test("the enumerator offers only the teamwork cast when every creature is too expensive") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cruel Alliance")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val casts = game.getLegalActions(1)
                    .filter { it.description.startsWith("Cast Cruel Alliance") }

                withClue("mana value 3 or less has no legal target, so the plain cast is not offered") {
                    casts.none { it.actionType == "CastSpell" } shouldBe true
                }

                val teamworkCast = casts.single { it.actionType == "CastWithKicker" }
                teamworkCast.description shouldBe "Cast Cruel Alliance (Teamwork 2)"
                teamworkCast.isAffordable shouldBe true
                teamworkCast.additionalCostInfo?.costType shouldBe "TapForTotalPower"
                withClue("the teamwork branch announces its own, unrestricted target") {
                    teamworkCast.validTargets.shouldNotBeNull() shouldContain wurm
                }
            }

            test("the teamwork cast is offered unaffordable when no creature can pay the cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cruel Alliance")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    // Player 1 controls no creature at all, so total power 2 is unreachable.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val casts = game.getLegalActions(1)
                    .filter { it.description.startsWith("Cast Cruel Alliance") }

                val plainCast = casts.single { it.actionType == "CastSpell" }
                withClue("the plain cast's filter excludes the mana value 6 Craw Wurm") {
                    plainCast.validTargets shouldBe listOf(bears)
                }

                val teamworkCast = casts.single { it.actionType == "CastWithKicker" }
                withClue("teamwork's tap cost cannot be paid, so the variant is greyed out — it " +
                    "stays on the list so the player can see what teamwork would ask for") {
                    teamworkCast.isAffordable shouldBe false
                }
            }
        }
    }
}
