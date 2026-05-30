package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for the split card Spite // Malice (CardLayout.SPLIT).
 *
 * Spite ({3}{U}, Instant): "Counter target noncreature spell."
 * Malice ({3}{B}, Instant): "Destroy target nonblack creature. It can't be regenerated."
 *
 * Each half is cast independently via `CastSpell(faceIndex = ...)`. Exercises:
 *  - Spite countering a noncreature spell, and being unable to target a creature spell.
 *  - Malice destroying a nonblack creature, refusing a black creature, and ignoring a
 *    regeneration shield (its "can't be regenerated" clause).
 */
class SpiteMaliceScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.sorcery("Test Cantrip", ManaCost.parse("{1}{U}"), "Draw a card.")
        )
        cardRegistry.register(
            CardDefinition.creature("Test Bear", ManaCost.parse("{1}{G}"), setOf(Subtype("Bear")), 2, 2)
        )
        cardRegistry.register(
            CardDefinition.creature("Green Zombie", ManaCost.parse("{1}{G}"), setOf(Subtype("Zombie")), 2, 2)
        )
        cardRegistry.register(
            CardDefinition.creature("Black Brute", ManaCost.parse("{1}{B}"), setOf(Subtype("Zombie")), 2, 2)
        )

        context("Spite (face 0)") {
            test("counters a noncreature spell") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spite // Malice")
                    .withCardInHand(2, "Test Cantrip")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Test Cantrip")
                game.execute(PassPriority(game.player2Id))

                val spiteId = cardInHand(game, game.player1Id, "Spite // Malice")
                val cantripOnStack = spellOnStack(game, "Test Cantrip")
                val result = game.execute(
                    CastSpell(game.player1Id, spiteId, targets = listOf(ChosenTarget.Spell(cantripOnStack)), faceIndex = 0)
                )
                withClue("Spite should be cast: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Test Cantrip should be countered (in Opponent's graveyard)") {
                    game.isInGraveyard(2, "Test Cantrip") shouldBe true
                }
            }

            test("cannot counter a creature spell") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spite // Malice")
                    .withCardInHand(2, "Test Bear")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Test Bear")
                game.execute(PassPriority(game.player2Id))

                val spiteId = cardInHand(game, game.player1Id, "Spite // Malice")
                val bearOnStack = spellOnStack(game, "Test Bear")
                val result = game.execute(
                    CastSpell(game.player1Id, spiteId, targets = listOf(ChosenTarget.Spell(bearOnStack)), faceIndex = 0)
                )
                withClue("Spite should not be castable against a creature spell") {
                    result.error shouldNotBe null
                }
            }
        }

        context("Malice (face 1)") {
            test("destroys a nonblack creature") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spite // Malice")
                    .withCardOnBattlefield(2, "Test Bear")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val maliceId = cardInHand(game, game.player1Id, "Spite // Malice")
                val bear = game.findPermanent("Test Bear")!!
                val result = game.execute(
                    CastSpell(game.player1Id, maliceId, targets = listOf(ChosenTarget.Permanent(bear)), faceIndex = 1)
                )
                withClue("Malice should be cast: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Test Bear (nonblack) should be destroyed") {
                    game.isOnBattlefield("Test Bear") shouldBe false
                    game.isInGraveyard(2, "Test Bear") shouldBe true
                }
            }

            test("cannot target a black creature") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spite // Malice")
                    .withCardOnBattlefield(2, "Black Brute")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val maliceId = cardInHand(game, game.player1Id, "Spite // Malice")
                val brute = game.findPermanent("Black Brute")!!
                val result = game.execute(
                    CastSpell(game.player1Id, maliceId, targets = listOf(ChosenTarget.Permanent(brute)), faceIndex = 1)
                )
                withClue("Malice should not be castable against a black creature") {
                    result.error shouldNotBe null
                }
                withClue("Black Brute should still be on the battlefield") {
                    game.isOnBattlefield("Black Brute") shouldBe true
                }
            }

            test("can't be regenerated — a regeneration shield does not save the creature") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(2, "Boneknitter")     // {1}{B}: Regenerate target Zombie
                    .withCardOnBattlefield(2, "Green Zombie")     // nonblack Zombie to protect
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInHand(1, "Spite // Malice")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val zombieId = game.findPermanent("Green Zombie")!!

                // Opponent puts a regeneration shield on the Green Zombie.
                val ability = cardRegistry.getCard("Boneknitter")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(zombieId))
                    )
                )
                game.resolveStack()
                game.execute(PassPriority(game.player2Id))

                // Caster destroys it with Malice — the shield must not save it.
                val maliceId = cardInHand(game, game.player1Id, "Spite // Malice")
                val result = game.execute(
                    CastSpell(game.player1Id, maliceId, targets = listOf(ChosenTarget.Permanent(zombieId)), faceIndex = 1)
                )
                withClue("Malice should be cast: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Green Zombie should be destroyed despite the regeneration shield") {
                    game.isOnBattlefield("Green Zombie") shouldBe false
                    game.isInGraveyard(2, "Green Zombie") shouldBe true
                }
            }
        }
    }

    private fun cardInHand(game: TestGame, playerId: EntityId, name: String): EntityId =
        game.state.getHand(playerId).first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun spellOnStack(game: TestGame, name: String): EntityId =
        game.state.stack.first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }
}
