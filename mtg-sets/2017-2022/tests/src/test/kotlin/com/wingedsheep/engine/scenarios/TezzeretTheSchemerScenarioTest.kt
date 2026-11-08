package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Tezzeret the Schemer (AER #137, {2}{U}{B}, loyalty 5).
 *
 *   +1: Create a colorless artifact token named Etherium Cell with "{T}, Sacrifice this token:
 *       Add one mana of any color."
 *   −2: Target creature gets +X/-X until end of turn, where X is the number of artifacts you control.
 *   −7: You get an emblem with "At the beginning of combat on your turn, target artifact you
 *       control becomes an artifact creature with base power and toughness 5/5."
 *
 * Covers the new Etherium Cell predefined token (and its mana ability), the −2's X being locked
 * as it resolves, and the emblem's indefinite base-5/5 animation on both a noncreature artifact
 * and an artifact creature.
 */
class TezzeretTheSchemerScenarioTest : ScenarioTestBase() {

    private val ironTrinket = CardDefinition.artifact(
        name = "Iron Trinket",
        manaCost = ManaCost.parse("{1}")
    )

    private val testOgre = card("Test Ogre") {
        manaCost = "{4}"
        typeLine = "Creature — Ogre"
        power = 3
        toughness = 5
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Tezzeret the Schemer")!!.script.activatedAbilities[index]

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        cardRegistry.register(ironTrinket)
        cardRegistry.register(testOgre)

        context("Tezzeret the Schemer") {

            test("+1 creates an Etherium Cell artifact token that sacrifices for one mana of any color") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret the Schemer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
                setLoyalty(game, tezzeret, 5)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tezzeret, abilityId = ability(0).id)
                ).error shouldBe null
                game.resolveStack()

                loyalty(game, tezzeret) shouldBe 6
                val cell = game.findPermanent("Etherium Cell")
                withClue("the +1 makes a colorless, noncreature artifact token named Etherium Cell") {
                    cell shouldNotBe null
                    game.state.getEntity(cell!!)?.has<TokenComponent>() shouldBe true
                    game.state.projectedState.hasType(cell, "ARTIFACT") shouldBe true
                    game.state.projectedState.isCreature(cell) shouldBe false
                }

                val cellAbility = cardRegistry.getCard("Etherium Cell")!!.script.activatedAbilities.single()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cell!!,
                        abilityId = cellAbility.id,
                        manaColorChoice = Color.BLACK
                    )
                ).error shouldBe null

                withClue("tapping and sacrificing the Cell adds one mana of the chosen color") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black shouldBe 1
                    game.findPermanent("Etherium Cell") shouldBe null
                }
            }

            test("−2 gives +X/-X where X is the artifacts you control, locked in as it resolves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret the Schemer")
                    .withCardOnBattlefield(1, "Iron Trinket")
                    .withCardOnBattlefield(1, "Palladium Myr")
                    .withCardInHand(1, "Iron Trinket")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Test Ogre")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
                val ogre = game.findPermanent("Test Ogre")!!
                setLoyalty(game, tezzeret, 5)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tezzeret,
                        abilityId = ability(1).id,
                        targets = listOf(ChosenTarget.Permanent(ogre))
                    )
                ).error shouldBe null
                game.resolveStack()

                loyalty(game, tezzeret) shouldBe 3
                withClue("two artifacts (Trinket + Myr) → the 3/5 Ogre becomes 5/3") {
                    val pv = game.state.projectedState.getProjectedValues(ogre)
                    pv?.power shouldBe 5
                    pv?.toughness shouldBe 3
                }

                // A third artifact arriving afterwards doesn't change X (2017-02-09 ruling).
                game.castSpell(1, "Iron Trinket").error shouldBe null
                game.resolveStack()
                game.findPermanents("Iron Trinket").size shouldBe 2

                withClue("X was counted on resolution, so the Ogre stays 5/3") {
                    val pv = game.state.projectedState.getProjectedValues(ogre)
                    pv?.power shouldBe 5
                    pv?.toughness shouldBe 3
                }
            }

            test("−7 emblem turns a noncreature artifact into a 5/5 artifact creature indefinitely") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret the Schemer")
                    .withCardOnBattlefield(1, "Iron Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
                val trinket = game.findPermanent("Iron Trinket")!!
                setLoyalty(game, tezzeret, 7)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tezzeret, abilityId = ability(2).id)
                ).error shouldBe null
                game.resolveStack()

                withClue("the emblem outlives Tezzeret, who goes to 0 loyalty") {
                    game.findPermanent("Tezzeret the Schemer") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(trinket))
                game.resolveStack()

                withClue("Iron Trinket is now a 5/5 artifact creature") {
                    val projected = game.state.projectedState
                    projected.isCreature(trinket) shouldBe true
                    projected.hasType(trinket, "ARTIFACT") shouldBe true
                    projected.getProjectedValues(trinket)?.power shouldBe 5
                    projected.getProjectedValues(trinket)?.toughness shouldBe 5
                }

                // "The effect of the emblem's triggered ability lasts indefinitely."
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("still a 5/5 creature on the opponent's next turn") {
                    game.state.activePlayerId shouldBe game.player2Id
                    val projected = game.state.projectedState
                    projected.isCreature(trinket) shouldBe true
                    projected.getProjectedValues(trinket)?.power shouldBe 5
                    projected.getProjectedValues(trinket)?.toughness shouldBe 5
                }
            }

            test("−7 emblem sets an artifact creature's base power and toughness to 5/5") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret the Schemer")
                    .withCardOnBattlefield(1, "Palladium Myr")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
                val myr = game.findPermanent("Palladium Myr")!!
                setLoyalty(game, tezzeret, 7)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tezzeret, abilityId = ability(2).id)
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(myr))
                game.resolveStack()

                withClue("the 2/2 Palladium Myr becomes 5/5 instead of its normal P/T") {
                    val pv = game.state.projectedState.getProjectedValues(myr)
                    pv?.power shouldBe 5
                    pv?.toughness shouldBe 5
                }
            }
        }
    }
}
