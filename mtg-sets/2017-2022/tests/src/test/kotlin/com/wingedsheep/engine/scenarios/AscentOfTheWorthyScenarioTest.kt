package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Ascent of the Worthy (KHM #202).
 *
 *   I, II — Choose a creature you control. Until your next turn, all damage that would be dealt to
 *           creatures you control is dealt to that creature instead.
 *   III — Return target creature card from your graveyard to the battlefield with a flying counter
 *         on it. That creature is an Angel Warrior in addition to its other types.
 *
 * Pins the rulings: the choice is made on resolution; damage to the chosen creature stays on it;
 * a chosen creature that has left the battlefield redirects nothing; combat damage is redirected
 * too; and the shield covers only the Saga controller's creatures, until their next turn.
 */
class AscentOfTheWorthyScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        fun builder() = scenario()
            .withPlayers("Worthy", "Opponent")
            .withCardInHand(1, "Ascent of the Worthy")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(2, "Mountain", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(6) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }

        /** Cast the Saga; chapter I asks for the soaking creature as it resolves. */
        fun TestGame.castAndChoose(soak: EntityId) {
            castSpell(1, "Ascent of the Worthy").error shouldBe null
            resolveStack()
            withClue("chapter I chooses the creature on resolution") {
                getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            selectTargets(listOf(soak)).error shouldBe null
            resolveStack()
        }

        context("chapters I and II") {
            test("damage to another creature you control is dealt to the chosen creature instead") {
                val game = builder().withCardInHand(2, "Shock").build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                game.passPriority()
                game.castSpell(2, "Shock", bears).error shouldBe null
                game.resolveStack()

                withClue("the Bears took nothing") { game.damageOn(bears) shouldBe 0 }
                withClue("the Hill Giant took the 2 instead") { game.damageOn(giant) shouldBe 2 }
            }

            test("damage to the chosen creature itself stays on it") {
                val game = builder().withCardInHand(2, "Shock").build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                game.passPriority()
                game.castSpell(2, "Shock", giant).error shouldBe null
                game.resolveStack()

                game.damageOn(giant) shouldBe 2
                game.damageOn(bears) shouldBe 0
            }

            test("players and the opponent's creatures are not covered") {
                val game = builder()
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Shock")
                    .withCardOnBattlefield(2, "Giant Spider")
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                val spider = game.findPermanent("Giant Spider")!!
                game.castAndChoose(giant)

                game.passPriority()
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.resolveStack()
                game.passPriority()
                game.castSpell(2, "Shock", spider).error shouldBe null
                game.resolveStack()

                withClue("damage to the Saga's controller is not redirected") { game.getLifeTotal(1) shouldBe 18 }
                withClue("damage to an opponent's creature is not redirected") { game.damageOn(spider) shouldBe 2 }
                game.damageOn(giant) shouldBe 0
            }

            test("if an opponent gains control of the chosen creature, your creatures' damage still goes to it") {
                val game = builder()
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withCardInHand(2, "Threaten")
                    .withCardInHand(2, "Shock")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.castSpell(2, "Threaten", giant).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(giant) shouldBe game.player2Id

                game.castSpell(2, "Shock", bears).error shouldBe null
                game.resolveStack()
                withClue("\"creatures you control\" means the Saga controller's, and the stolen Giant still soaks it") {
                    game.damageOn(bears) shouldBe 0
                    game.damageOn(giant) shouldBe 2
                }
            }

            test("a chosen creature that has left the battlefield redirects nothing") {
                val game = builder()
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(2, "Shock")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                game.passPriority()
                game.castSpell(2, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()
                game.findPermanent("Hill Giant") shouldBe null

                game.passPriority()
                game.castSpell(2, "Shock", bears).error shouldBe null
                game.resolveStack()
                withClue("with the Giant gone the Shock stays on the Bears and kills them") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }

            test("with no creature to choose the chapter does nothing") {
                val game = scenario()
                    .withPlayers("Worthy", "Opponent")
                    .withCardInHand(1, "Ascent of the Worthy")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                    .build()

                game.castSpell(1, "Ascent of the Worthy").error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
                game.state.stack.isEmpty() shouldBe true
                game.findPermanent("Ascent of the Worthy").shouldNotBeNull()
            }

            test("a flickered chosen creature is a new object and soaks nothing") {
                val game = builder()
                    .withCardInHand(1, "Cloudshift")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Shock")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                game.castSpell(1, "Cloudshift", giant).error shouldBe null
                game.resolveStack()
                val returnedGiant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.passPriority()
                game.castSpell(2, "Shock", bears).error shouldBe null
                game.resolveStack()
                withClue("\"that creature\" left the battlefield, so the Shock stays on the Bears") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.damageOn(returnedGiant) shouldBe 0
                }
            }

            test("a creature that enters after the chapter resolves is covered too") {
                val game = builder()
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(2, "Shock")
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                val before = game.findAllPermanents("Grizzly Bears").toSet()
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                val newBears = (game.findAllPermanents("Grizzly Bears").toSet() - before).single()

                game.passPriority()
                game.castSpell(2, "Shock", newBears).error shouldBe null
                game.resolveStack()
                game.damageOn(newBears) shouldBe 0
                game.damageOn(giant) shouldBe 2
            }

            test("combat damage on the opponent's turn is redirected, and the shield ends as your next turn begins") {
                val game = builder()
                    .withCardOnBattlefield(2, "Gray Ogre")
                    .withCardInHand(2, "Shock")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(giant)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.activePlayerId shouldBe game.player2Id
                game.declareAttackers(mapOf("Gray Ogre" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Gray Ogre"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the Ogre's combat damage to the blocking Bears went to the Giant") {
                    game.damageOn(bears) shouldBe 0
                    game.damageOn(giant) shouldBe 2
                }
                withClue("the Bears' own damage still hit the Ogre, so it died") {
                    game.findPermanent("Gray Ogre") shouldBe null
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.passPriority()
                game.castSpell(2, "Shock", bears).error shouldBe null
                game.resolveStack()
                withClue("it is now the Saga controller's turn, so the Shock stays on the Bears") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }
        }

        context("chapter III") {
            test("returns a creature card with a flying counter as an Angel Warrior") {
                val game = scenario()
                    .withPlayers("Worthy", "Opponent")
                    .withCardOnBattlefield(1, "Ascent of the Worthy")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                    .build()
                val saga = game.findPermanent("Ascent of the Worthy")!!
                game.state = game.state.updateEntity(saga) { container ->
                    container.with(CountersComponent().withAdded(CounterType.LORE, 2))
                        .with(SagaComponent(triggeredChapters = setOf(1, 2)))
                }
                val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bearsCard)).error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val projected = game.state.projectedState
                game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.FLYING) shouldBe 1
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
                withClue("an Angel Warrior in addition to its other types") {
                    projected.hasSubtype(bears, "Angel") shouldBe true
                    projected.hasSubtype(bears, "Warrior") shouldBe true
                    projected.hasSubtype(bears, "Bear") shouldBe true
                }
                withClue("the Saga is sacrificed after chapter III") {
                    game.findPermanent("Ascent of the Worthy") shouldBe null
                }
            }
        }
    }
}
