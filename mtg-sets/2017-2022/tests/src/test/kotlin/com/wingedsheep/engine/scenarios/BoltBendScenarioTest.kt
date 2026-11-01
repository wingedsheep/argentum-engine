package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bolt Bend (WAR #115) — {3}{R} Instant.
 *
 *   This spell costs {3} less to cast if you control a creature with power 4 or greater.
 *   Change the target of target spell or ability with a single target.
 *
 * Covers a spell, an activated ability, and the ability's own controller scoping the new target:
 * an opponent's "target creature you control" ability can only be bent onto *their* creatures.
 */
class BoltBendScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.castBoltBendAt(stackObject: EntityId) {
            val bend = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Bolt Bend"
            }
            execute(CastSpell(player1Id, bend, listOf(ChosenTarget.Spell(stackObject)))).error shouldBe null
        }

        fun TestGame.tryBoltBendAt(stackObject: EntityId): String? {
            val bend = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Bolt Bend"
            }
            return execute(CastSpell(player1Id, bend, listOf(ChosenTarget.Spell(stackObject)))).error
        }

        fun TestGame.activateFirstAbility(cardName: String, abilityIndex: Int, target: ChosenTarget): EntityId {
            val source = findPermanent(cardName)!!
            val abilityId = cardRegistry.getCard(cardName)!!.script.activatedAbilities[abilityIndex].id
            execute(ActivateAbility(playerId = player2Id, sourceId = source, abilityId = abilityId, targets = listOf(target)))
                .error shouldBe null
            return state.stack.last()
        }

        context("Bolt Bend") {

            test("redirects a spell's single target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bolt Bend")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                val bolt = game.state.stack.last()
                game.passPriority().error shouldBe null
                game.castBoltBendAt(bolt)

                game.resolveStack()
                game.selectCards(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("the Bolt hit its own caster instead") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("can't target a spell with no targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bolt Bend")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                val bears = game.state.stack.last()
                game.passPriority().error shouldBe null

                withClue("a spell without a single target is not a legal Bolt Bend target") {
                    game.tryBoltBendAt(bears) shouldNotBe null
                }
            }

            test("can't target a spell with several targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bolt Bend")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInHand(2, "Seeds of Strength")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val seeds = game.state.getHand(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Seeds of Strength"
                }
                game.execute(
                    CastSpell(
                        game.player2Id, seeds,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser), ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                val seedsOnStack = game.state.stack.last()
                game.passPriority().error shouldBe null

                withClue("a spell with three targets is not a legal Bolt Bend target") {
                    game.tryBoltBendAt(seedsOnStack) shouldNotBe null
                }
            }

            test("costs {3} less with a power-4 creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bolt Bend")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                val bolt = game.state.stack.last()
                game.passPriority().error shouldBe null
                game.castBoltBendAt(bolt)

                game.resolveStack()
                game.selectCards(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 17
            }

            test("redirects an activated ability's single target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bolt Bend")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ping = game.activateFirstAbility("Prodigal Sorcerer", 0, ChosenTarget.Player(game.player1Id))
                game.passPriority().error shouldBe null
                game.castBoltBendAt(ping)

                game.resolveStack()
                game.selectCards(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("the ping hit the Sorcerer's controller instead") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("an ability's new target is judged from the ability controller's side") {
                // Sunscape Apprentice: "{U}, {T}: Put target creature you control on top of its
                // owner's library." The opponent aims it at their own Grizzly Bears; bending it can
                // only pick another creature *they* control, never one of Bolt Bend's caster's.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bolt Bend")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Sunscape Apprentice", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val giant = game.findPermanent("Hill Giant")!!
                val ability = game.activateFirstAbility("Sunscape Apprentice", 1, ChosenTarget.Permanent(bears))
                game.passPriority().error shouldBe null
                game.castBoltBendAt(ability)

                game.resolveStack()
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("Expected a new-target selection; got ${game.getPendingDecision()}")
                withClue("only the ability controller's creatures are legal new targets") {
                    decision.options shouldContain courser
                    decision.options shouldNotContain giant
                }
                game.selectCards(listOf(courser)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Centaur Courser") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
