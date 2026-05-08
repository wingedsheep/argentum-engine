package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Dragon Throne of Tarkir.
 *
 * Dragon Throne of Tarkir: {4} Legendary Artifact — Equipment
 * Equipped creature has defender and "{2}, {T}: Other creatures you control gain trample
 * and get +X/+X until end of turn, where X is this creature's power."
 * Equip {3}
 */
class DragonThroneOfTarkirScenarioTest : ScenarioTestBase() {

    init {
        context("Dragon Throne of Tarkir") {

            test("equipped creature has defender") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dragon Throne of Tarkir")
                    .withCardOnBattlefield(1, "Alpine Grizzly", summoningSickness = false) // 4/2
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val throneId = game.findPermanent("Dragon Throne of Tarkir")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                // Equip the throne to Alpine Grizzly
                val cardDef = cardRegistry.getCard("Dragon Throne of Tarkir")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = throneId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )

                withClue("Equip activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Verify equipped
                val throneEntity = game.state.getEntity(throneId)!!
                val attachedTo = throneEntity.get<AttachedToComponent>()
                withClue("Throne should be attached to Alpine Grizzly") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe grizzlyId
                }

                // Verify Alpine Grizzly has defender in projected state
                val projected = game.state.projectedState
                withClue("Equipped creature should have defender") {
                    projected.getKeywords(grizzlyId).contains(Keyword.DEFENDER.name) shouldBe true
                }
            }

            test("activated ability gives other creatures trample and +X/+X") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dragon Throne of Tarkir")
                    .withCardOnBattlefield(1, "Alpine Grizzly", summoningSickness = false) // 4/2
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2
                    .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = false) // 2/2
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val throneId = game.findPermanent("Dragon Throne of Tarkir")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val seekerId = game.findPermanent("Glory Seeker")!!

                // Equip the throne to Alpine Grizzly
                val cardDef = cardRegistry.getCard("Dragon Throne of Tarkir")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = throneId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )
                game.resolveStack()

                // Get the granted activated ability
                val grantAbility = cardDef.script.staticAbilities
                    .filterIsInstance<GrantActivatedAbility>()
                    .first()
                val grantedAbilityId = grantAbility.ability.id

                // Activate the granted ability (costs {2}, tap)
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = grizzlyId,
                        abilityId = grantedAbilityId
                    )
                )

                withClue("Granted ability activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // Verify other creatures got +4/+4 (Alpine Grizzly's power is 4)
                val projected = game.state.projectedState

                withClue("Devoted Hero should be 5/6 (1/2 + 4/4)") {
                    projected.getPower(heroId) shouldBe 5
                    projected.getToughness(heroId) shouldBe 6
                }

                withClue("Glory Seeker should be 6/6 (2/2 + 4/4)") {
                    projected.getPower(seekerId) shouldBe 6
                    projected.getToughness(seekerId) shouldBe 6
                }

                // Verify trample
                withClue("Devoted Hero should have trample") {
                    projected.getKeywords(heroId).contains(Keyword.TRAMPLE.name) shouldBe true
                }
                withClue("Glory Seeker should have trample") {
                    projected.getKeywords(seekerId).contains(Keyword.TRAMPLE.name) shouldBe true
                }

                // Verify the equipped creature itself was NOT buffed (only "other" creatures)
                withClue("Alpine Grizzly should remain 4/2 (not buffed by its own ability)") {
                    projected.getPower(grizzlyId) shouldBe 4
                    projected.getToughness(grizzlyId) shouldBe 2
                }
            }
        }
    }
}
