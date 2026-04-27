package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Figure of Fable.
 *
 * {G/W} 1/1 Kithkin with three permanent activated abilities that transform it
 * into successively bigger creatures: Scout 2/3, Soldier 4/5, Avatar 7/8 with
 * protection from each opponent.
 */
class FigureOfFableScenarioTest : ScenarioTestBase() {

    private fun activateAbility(game: TestGame, abilityIndex: Int) {
        val sourceId = game.findPermanent("Figure of Fable")!!
        val cardDef = cardRegistry.getCard("Figure of Fable")!!
        val ability = cardDef.script.activatedAbilities[abilityIndex]
        val result = game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = sourceId,
                abilityId = ability.id
            )
        )
        withClue("Activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    private fun setMana(game: TestGame, white: Int = 0, green: Int = 0, colorless: Int = 0) {
        game.state = game.state.updateEntity(game.player1Id) { container ->
            container.with(ManaPoolComponent(white = white, green = green, colorless = colorless))
        }
    }

    init {
        context("Figure of Fable transformation chain") {

            test("first ability turns it into a 2/3 Kithkin Scout") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                projected.getPower(sourceId) shouldBe 2
                projected.getToughness(sourceId) shouldBe 3
                withClue("Should have Scout subtype") {
                    ("Scout" in projected.getSubtypes(sourceId)) shouldBe true
                }
                withClue("Should still be a Kithkin") {
                    ("Kithkin" in projected.getSubtypes(sourceId)) shouldBe true
                }
            }

            test("second ability does nothing if not yet a Scout (Scryfall ruling)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Should remain 1/1 — no Scout, condition fails") {
                    projected.getPower(sourceId) shouldBe 1
                    projected.getToughness(sourceId) shouldBe 1
                }
                withClue("Should not be a Soldier") {
                    ("Soldier" in projected.getSubtypes(sourceId)) shouldBe false
                }
            }

            test("Scout becomes a 4/5 Kithkin Soldier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                projected.getPower(sourceId) shouldBe 4
                projected.getToughness(sourceId) shouldBe 5
                ("Soldier" in projected.getSubtypes(sourceId)) shouldBe true
                withClue("Scout subtype is replaced") {
                    ("Scout" in projected.getSubtypes(sourceId)) shouldBe false
                }
            }

            test("Soldier becomes a 7/8 Kithkin Avatar with protection from each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                projected.getPower(sourceId) shouldBe 7
                projected.getToughness(sourceId) shouldBe 8
                ("Avatar" in projected.getSubtypes(sourceId)) shouldBe true
                withClue("Should have protection from each opponent") {
                    projected.hasKeyword(sourceId, "PROTECTION_FROM_EACH_OPPONENT") shouldBe true
                }
            }
        }

        context("Figure of Fable client-visible state") {

            test("ClientCard typeLine and subtypes reflect each transformation step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun clientCard() = game.getClientState(1).cards.values
                    .first { it.name == "Figure of Fable" }

                withClue("Base form should display Kithkin only") {
                    val card = clientCard()
                    card.subtypes shouldBe setOf("Kithkin")
                    card.typeLine shouldBe "Creature — Kithkin"
                }

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                withClue("After Scout transformation, subtypes are exactly Kithkin + Scout") {
                    val card = clientCard()
                    card.subtypes shouldBe setOf("Kithkin", "Scout")
                    card.typeLine shouldBe "Creature — Kithkin Scout"
                }

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                withClue("After Soldier transformation, subtypes are exactly Kithkin + Soldier (Scout replaced)") {
                    val card = clientCard()
                    card.subtypes shouldBe setOf("Kithkin", "Soldier")
                    card.typeLine shouldBe "Creature — Kithkin Soldier"
                }

                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                withClue("After Avatar transformation, subtypes are exactly Kithkin + Avatar (Soldier replaced)") {
                    val card = clientCard()
                    card.subtypes shouldBe setOf("Kithkin", "Avatar")
                    card.typeLine shouldBe "Creature — Kithkin Avatar"
                }
            }

            test("active-effect badge surfaces the current creature types and never duplicates") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun typeChangeBadges() = game.getClientState(1).cards.values
                    .first { it.name == "Figure of Fable" }
                    .activeEffects
                    .filter { it.icon == "type-change" }

                withClue("Base form has no type-change badge") {
                    typeChangeBadges() shouldBe emptyList()
                }

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                withClue("Scout transformation surfaces exactly one type-change badge") {
                    val badges = typeChangeBadges()
                    badges.size shouldBe 1
                    badges[0].name shouldBe "Kithkin Scout"
                }

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                withClue("Soldier transformation replaces the Scout badge — no duplicates") {
                    val badges = typeChangeBadges()
                    badges.size shouldBe 1
                    badges[0].name shouldBe "Kithkin Soldier"
                }

                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                withClue("Avatar transformation again shows only one badge") {
                    val badges = typeChangeBadges()
                    badges.size shouldBe 1
                    badges[0].name shouldBe "Kithkin Avatar"
                }

                // Activating ability 2 from Avatar fizzles — badge should still show Avatar
                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                withClue("A fizzled activation does not change or duplicate the badge") {
                    val badges = typeChangeBadges()
                    badges.size shouldBe 1
                    badges[0].name shouldBe "Kithkin Avatar"
                }
            }

            test("activated abilities do not offer the repeat-activation prompt") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Get Scout state so all three abilities are mana-affordable in their normal form
                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                // Untap lands so legal-action enumeration sees full mana availability
                game.state = game.state.getBattlefield()
                    .filter { id ->
                        val name = game.state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
                        name == "Forest" || name == "Plains"
                    }
                    .fold(game.state) { acc, id ->
                        acc.updateEntity(id) { container ->
                            container.without<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()
                        }
                    }

                val enumerator = LegalActionEnumerator.create(cardRegistry)
                val legalActions = enumerator.enumerate(game.state, game.player1Id)

                val activations = legalActions.filter {
                    it.action is ActivateAbility &&
                        (it.action as ActivateAbility).sourceId == game.findPermanent("Figure of Fable")
                }

                withClue("Found ${activations.size} Figure of Fable activations") {
                    activations.isNotEmpty() shouldBe true
                }
                activations.forEach { la ->
                    withClue("Activation ${la.description} should not offer repeat-activation prompt") {
                        la.maxRepeatableActivations shouldBe null
                    }
                }
            }

            test("chosenCreatureType is not populated for Figure of Fable transformations") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun clientCard() = game.getClientState(1).cards.values
                    .first { it.name == "Figure of Fable" }

                clientCard().chosenCreatureType shouldBe null

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                withClue("Scout transform must not produce a stray chosen-creature-type label") {
                    clientCard().chosenCreatureType shouldBe null
                }

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                withClue("Soldier transform must not produce a stray chosen-creature-type label") {
                    clientCard().chosenCreatureType shouldBe null
                }
            }
        }

        context("Figure of Fable evolution gating (cannot skip steps or evolve backwards)") {

            test("third ability fizzles from base form (skipping Scout and Soldier)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Base 1/1 — third ability requires Soldier, condition fails") {
                    projected.getPower(sourceId) shouldBe 1
                    projected.getToughness(sourceId) shouldBe 1
                }
                ("Avatar" in projected.getSubtypes(sourceId)) shouldBe false
                projected.hasKeyword(sourceId, "PROTECTION_FROM_EACH_OPPONENT") shouldBe false
            }

            test("third ability fizzles from Scout form (skipping Soldier)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Should still be a 2/3 Scout, not an Avatar") {
                    projected.getPower(sourceId) shouldBe 2
                    projected.getToughness(sourceId) shouldBe 3
                }
                ("Scout" in projected.getSubtypes(sourceId)) shouldBe true
                ("Avatar" in projected.getSubtypes(sourceId)) shouldBe false
                projected.hasKeyword(sourceId, "PROTECTION_FROM_EACH_OPPONENT") shouldBe false
            }

            test("second ability fizzles from Soldier form (cannot re-trigger forward step)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()

                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                // Now a Soldier — re-activating ability 2 should fizzle (Soldier is not a Scout)
                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Should still be a 4/5 Soldier — second ability is a no-op when not a Scout") {
                    projected.getPower(sourceId) shouldBe 4
                    projected.getToughness(sourceId) shouldBe 5
                }
                ("Soldier" in projected.getSubtypes(sourceId)) shouldBe true
                ("Scout" in projected.getSubtypes(sourceId)) shouldBe false
            }

            test("second ability fizzles from Avatar form (cannot evolve backwards to Soldier)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()
                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()
                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                // Avatar — second ability requires Scout; should fizzle
                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Should still be a 7/8 Avatar — second ability fizzles when not a Scout") {
                    projected.getPower(sourceId) shouldBe 7
                    projected.getToughness(sourceId) shouldBe 8
                }
                ("Avatar" in projected.getSubtypes(sourceId)) shouldBe true
                ("Soldier" in projected.getSubtypes(sourceId)) shouldBe false
                withClue("Avatar still has protection from each opponent") {
                    projected.hasKeyword(sourceId, "PROTECTION_FROM_EACH_OPPONENT") shouldBe true
                }
            }

            test("third ability fizzles from Avatar form (cannot re-trigger forward step)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Figure of Fable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setMana(game, green = 1)
                activateAbility(game, 0)
                game.resolveStack()
                setMana(game, green = 2, colorless = 1)
                activateAbility(game, 1)
                game.resolveStack()
                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                // Avatar — third ability requires Soldier; should fizzle
                setMana(game, green = 3, colorless = 3)
                activateAbility(game, 2)
                game.resolveStack()

                val sourceId = game.findPermanent("Figure of Fable")!!
                val projected = game.state.projectedState
                withClue("Should still be a 7/8 Avatar") {
                    projected.getPower(sourceId) shouldBe 7
                    projected.getToughness(sourceId) shouldBe 8
                }
                ("Avatar" in projected.getSubtypes(sourceId)) shouldBe true
            }
        }
    }
}
