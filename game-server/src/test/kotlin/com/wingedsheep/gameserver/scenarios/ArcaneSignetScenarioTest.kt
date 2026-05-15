package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Arcane Signet.
 *
 * Card reference:
 * - Arcane Signet ({2}): Artifact
 *   "{T}: Add one mana of any color in your commander's color identity."
 *
 * Scryfall rulings (2020-11-10):
 * - With two commanders, the union of their color identities is available.
 * - Without a commander, the ability produces no mana.
 * - A colorless commander yields no mana (not {C}).
 */
class ArcaneSignetScenarioTest : ScenarioTestBase() {

    init {
        context("Arcane Signet mana ability") {
            test("produces a color in the commander's color identity (Bello → R/G)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arcane Signet")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.designateCommanderInCommandZone(1, "Bello, Bard of the Brambles")

                val signet = game.findPermanent("Arcane Signet")!!
                val ability = cardRegistry.getCard("Arcane Signet")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = signet,
                        abilityId = ability.id,
                        manaColorChoice = Color.RED,
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val pool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should have 1 red mana from Bello's R/G identity") {
                    pool shouldNotBe null
                    pool!!.red shouldBe 1
                }
            }

            test("falls back to an available color when the requested color is outside the identity") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arcane Signet")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.designateCommanderInCommandZone(1, "Bello, Bard of the Brambles")

                val signet = game.findPermanent("Arcane Signet")!!
                val ability = cardRegistry.getCard("Arcane Signet")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = signet,
                        abilityId = ability.id,
                        manaColorChoice = Color.BLUE, // not in Bello's R/G identity
                    )
                )

                result.isSuccess shouldBe true
                val pool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Blue is not in R/G identity — no blue produced") {
                    pool shouldNotBe null
                    pool!!.blue shouldBe 0
                }
                withClue("Some color in the identity should be produced") {
                    pool!!.red + pool.green shouldBe 1
                }
            }

            test("produces no mana when the controller has no commander") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arcane Signet")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val signet = game.findPermanent("Arcane Signet")!!
                val ability = cardRegistry.getCard("Arcane Signet")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = signet,
                        abilityId = ability.id,
                        manaColorChoice = Color.WHITE,
                    )
                )

                withClue("Ability resolves but produces no mana") {
                    result.isSuccess shouldBe true
                }
                val pool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                if (pool != null) {
                    pool.white shouldBe 0
                    pool.blue shouldBe 0
                    pool.black shouldBe 0
                    pool.red shouldBe 0
                    pool.green shouldBe 0
                    pool.colorless shouldBe 0
                }
            }
        }
    }

    /**
     * Designate an existing or freshly-created card as the player's commander, placing it in
     * the command zone and registering it on the player. Tests in non-real-game scenarios
     * don't run the [com.wingedsheep.engine.core.GameInitializer] commander setup path.
     */
    private fun TestGame.designateCommanderInCommandZone(playerNumber: Int, commanderName: String) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val def = cardRegistry.getCard(commanderName)
            ?: error("Card not found in registry: $commanderName")

        // Create the commander entity in the command zone.
        val commanderId = com.wingedsheep.sdk.model.EntityId.of("commander-$commanderName")
        val cardComponent = CardComponent(
            cardDefinitionId = def.name,
            name = def.name,
            manaCost = def.manaCost,
            typeLine = def.typeLine,
            oracleText = def.oracleText,
            colors = def.colors,
            baseKeywords = def.keywords,
            baseFlags = def.flags,
            baseStats = def.creatureStats,
            ownerId = playerId,
            spellEffect = def.spellEffect,
            imageUri = def.metadata.imageUri,
        )
        val container = com.wingedsheep.engine.state.ComponentContainer.of(
            cardComponent,
            com.wingedsheep.engine.state.components.identity.OwnerComponent(playerId),
            CommanderComponent(ownerId = playerId),
        )
        state = state.withEntity(commanderId, container)
        state = state.addToZone(
            com.wingedsheep.engine.state.ZoneKey(playerId, com.wingedsheep.sdk.core.Zone.COMMAND),
            commanderId,
        )

        // Attach the commander registry to the player.
        state = state.updateEntity(playerId) { c ->
            val existing = c.get<CommanderRegistryComponent>()
            val ids = (existing?.commanderIds ?: emptyList()) + commanderId
            c.with(CommanderRegistryComponent(ids))
        }
    }
}
