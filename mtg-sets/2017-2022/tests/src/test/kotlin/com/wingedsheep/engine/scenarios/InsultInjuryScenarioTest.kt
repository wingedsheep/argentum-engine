package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Insult // Injury (AKH).
 *
 * Insult {2}{R}: "Damage can't be prevented this turn. If a source you control would deal damage this
 * turn, it deals double that damage instead."
 * Injury {2}{R}, Aftermath: "Injury deals 2 damage to target creature and 2 damage to target player or
 * planeswalker."
 *
 * Insult's doubling is an until-end-of-turn replacement that outlives the spell: Injury cast right
 * after it from the graveyard deals 4 and 4, your attacker's combat damage is doubled while the
 * opponent's blocker's is not, and two Insults quadruple (each doubles once — the card's ruling).
 */
class InsultInjuryScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.base(): ScenarioBuilder = this
        .withPlayers("Caster", "Opponent")
        .withLandsOnBattlefield(1, "Mountain", 9)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.castInsult() {
        val card = findCardsInHand(1, "Insult // Injury").first()
        execute(CastSpell(player1Id, card, faceIndex = 0)).error.shouldBeNull()
        resolveStack()
    }

    private fun TestGame.castInjury(creature: EntityId) {
        val card = findCardsInGraveyard(1, "Insult // Injury").first()
        execute(
            CastSpell(
                player1Id, card,
                listOf(ChosenTarget.Permanent(creature), ChosenTarget.Player(player2Id)),
                faceIndex = 1
            )
        ).error.shouldBeNull()
        resolveStack()
    }

    init {
        test("Injury after Insult deals double damage to both targets and is exiled") {
            val game = scenario().base()
                .withCardInHand(1, "Insult // Injury")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()

            game.castInsult()

            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.castInjury(giant)

            withClue("Injury's 2 damage is doubled to 4 — enough to kill the 3/3") {
                game.findPermanent("Hill Giant").shouldBeNull()
            }
            withClue("Injury's 2 damage to the player is doubled to 4") {
                game.getLifeTotal(2) shouldBe 16
            }
            withClue("Injury, cast from the graveyard, is exiled") {
                game.isInExile(1, "Insult // Injury") shouldBe true
            }
        }

        test("without Insult, Injury deals its printed 2 and 2") {
            val game = scenario().base()
                .withCardInGraveyard(1, "Insult // Injury")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()

            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.castInjury(giant)

            game.findPermanent("Hill Giant").shouldNotBeNull()
            game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
            game.getLifeTotal(2) shouldBe 18
        }

        test("only sources you control deal double: your attacker doubles, the opponent's blocker does not") {
            val game = scenario().base()
                .withCardInHand(1, "Insult // Injury")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()

            game.castInsult()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Craw Wurm" to 2, "Grizzly Bears" to 2)).error.shouldBeNull()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Craw Wurm"))).error.shouldBeNull()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("the unblocked Bears' 2 combat damage is doubled to 4") {
                game.getLifeTotal(2) shouldBe 16
            }
            withClue("the blocked Wurm kills the Giant") {
                game.findPermanent("Hill Giant").shouldBeNull()
            }
            val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
            withClue("the opponent's Giant deals its unmodified 3 — the 6/4 Wurm survives") {
                game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 3
            }
        }

        test("two Insults quadruple your damage") {
            val game = scenario().base()
                .withCardsInHand(1, "Insult // Injury", 2)
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()

            game.castInsult()
            game.castInsult()

            val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
            game.castInjury(wurm)

            withClue("2 doubled twice is 8 to each target") {
                game.findPermanent("Craw Wurm").shouldBeNull()
                game.getLifeTotal(2) shouldBe 12
            }
        }
    }
}
