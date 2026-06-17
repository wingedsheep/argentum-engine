package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Grand Abolisher (Magic 2012 #19):
 * "During your turn, your opponents can't cast spells or activate abilities of artifacts,
 * creatures, or enchantments."
 *
 * Two reusable continuous statics, each scoped to opponents during the controller's turn:
 *  - the cast clause [com.wingedsheep.sdk.scripting.PlayersCantCastSpells] (proven via the
 *    `reasonCannotCast` chokepoint, which is independent of mana availability), and
 *  - the activate clause [com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities]
 *    (proven via legal-action enumeration + handler rejection).
 *
 * Edge cases covered: the filter spares lands (mana abilities) and planeswalker loyalty by
 * matching only artifacts/creatures/enchantments; the controller's own abilities are untouched;
 * and the lock lifts on the opponent's own turn (your-turn-only).
 */
class GrandAbolisherScenarioTest : ScenarioTestBase() {

    private fun enumerateFor(game: TestGame, playerId: com.wingedsheep.sdk.model.EntityId) =
        LegalActionEnumerator.create(cardRegistry).enumerate(game.state, playerId)

    init {
        context("Grand Abolisher's activate clause") {
            test("an opponent's creature ability is not enumerated during the controller's turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val manaAbility = cardRegistry.getCard("Llanowar Elves")!!
                    .script.activatedAbilities.first()

                val activation = enumerateFor(game, game.player2Id).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == manaAbility.id
                }
                withClue("Opponent's creature mana ability should be blocked during Grand Abolisher's turn") {
                    activation shouldBe null
                }
            }

            test("the handler rejects an opponent's creature-ability activation during the controller's turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val manaAbility = cardRegistry.getCard("Llanowar Elves")!!
                    .script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(playerId = game.player2Id, sourceId = elves, abilityId = manaAbility.id)
                )
                withClue("Engine should reject the opponent's activation while Grand Abolisher locks their turn") {
                    (result.error != null) shouldBe true
                }
            }

            test("an opponent's LAND mana ability is unaffected (filter excludes lands)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                val activation = enumerateFor(game, game.player2Id).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == forest
                }
                withClue("Forest's mana ability should remain available — the lock only hits artifacts/creatures/enchantments") {
                    activation shouldNotBe null
                }
            }

            test("the controller's own creature ability is unaffected") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val manaAbility = cardRegistry.getCard("Llanowar Elves")!!
                    .script.activatedAbilities.first()

                val activation = enumerateFor(game, game.player1Id).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == manaAbility.id
                }
                withClue("The controller may still activate their own creatures' abilities") {
                    activation shouldNotBe null
                }
            }

            test("an opponent's creature ability works again on the OPPONENT's own turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val manaAbility = cardRegistry.getCard("Llanowar Elves")!!
                    .script.activatedAbilities.first()

                val activation = enumerateFor(game, game.player2Id).find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == manaAbility.id
                }
                withClue("The lock is your-turn-only, so the opponent may activate on their own turn") {
                    activation shouldNotBe null
                }
            }
        }

        context("Grand Abolisher's cast clause") {
            test("an opponent cannot cast a spell during the controller's turn, but the controller can") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentBolt = game.state.getZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player2Id, com.wingedsheep.sdk.core.Zone.HAND)
                ).first()
                val controllerBolt = game.state.getZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND)
                ).first()

                val utils = CastPermissionUtils(cardRegistry, PredicateEvaluator(), ConditionEvaluator())
                withClue("Opponent is locked out of casting during Grand Abolisher's turn") {
                    utils.reasonCannotCast(game.state, game.player2Id, opponentBolt) shouldNotBe null
                }
                withClue("The controller casts freely on their own turn") {
                    utils.reasonCannotCast(game.state, game.player1Id, controllerBolt) shouldBe null
                }
            }

            test("the opponent may cast on their own turn (your-turn-only)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grand Abolisher")
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentBolt = game.state.getZone(
                    com.wingedsheep.engine.state.ZoneKey(game.player2Id, com.wingedsheep.sdk.core.Zone.HAND)
                ).first()

                val utils = CastPermissionUtils(cardRegistry, PredicateEvaluator(), ConditionEvaluator())
                withClue("The cast lock is your-turn-only, so the opponent casts freely on their own turn") {
                    utils.reasonCannotCast(game.state, game.player2Id, opponentBolt) shouldBe null
                }
            }
        }
    }
}
