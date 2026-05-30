package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for a batch of Invasion (INV) cards:
 *  - Liberate ({1}{W} Instant; exile your creature, return at next end step)
 *  - Mourning ({1}{B} Aura; enchanted creature gets -2/-0; {B}: bounce this Aura)
 *  - Obliterate ({6}{R}{R} Sorcery; can't be countered; destroy all artifacts/creatures/lands)
 *  - Rout ({3}{W}{W} Sorcery; optional flash for {2}; destroy all creatures)
 *  - Winnow ({1}{W} Instant; destroy target nonland permanent if a same-named permanent exists; draw)
 */
class InvasionDestructionBatchScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Liberate") {
            test("exiles your creature, returning it at the next end step") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Liberate")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2) // {1}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearId = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Liberate", bearId)
                withClue("Liberate should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears should be exiled while Liberate resolves") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Grizzly Bears should return to the battlefield at the end step") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("Mourning") {
            test("gives enchanted creature -2/-0 and can bounce itself for {B}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Mourning")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 to weaken
                    .withLandsOnBattlefield(1, "Swamp", 3) // {1}{B} cast + {B} bounce
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearId = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Mourning", bearId)
                withClue("Mourning should attach: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val weakened = projector.project(game.state)
                withClue("Enchanted Grizzly Bears should be 0/2 after -2/-0") {
                    weakened.getPower(bearId) shouldBe 0
                    weakened.getToughness(bearId) shouldBe 2
                }

                val mourningId = game.findPermanent("Mourning")!!
                val bounceAbility = cardRegistry.getCard("Mourning")!!.script.activatedAbilities[0]
                val bounce = game.execute(ActivateAbility(game.player1Id, mourningId, bounceAbility.id, emptyList()))
                withClue("Bounce should succeed: ${bounce.error}") { bounce.error shouldBe null }
                game.resolveStack()

                withClue("Mourning should be back in its owner's hand") {
                    game.isInHand(1, "Mourning") shouldBe true
                    game.isOnBattlefield("Mourning") shouldBe false
                }
                val restored = projector.project(game.state)
                withClue("Grizzly Bears should be 2/2 again once Mourning leaves") {
                    restored.getPower(bearId) shouldBe 2
                }
            }
        }

        context("Obliterate") {
            test("destroys all artifacts, creatures, and lands") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Obliterate")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sparring Golem") // artifact creature
                    .withLandsOnBattlefield(1, "Mountain", 8) // {6}{R}{R}
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Obliterate")
                game.resolveStack()

                withClue("All creatures should be destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Sparring Golem") shouldBe false
                }
                withClue("All lands should be destroyed") {
                    game.isOnBattlefield("Mountain") shouldBe false
                    game.isOnBattlefield("Forest") shouldBe false
                }
            }

            test("can't be countered: Undermine fails to stop it and the board still wipes") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Obliterate")
                    .withCardInHand(2, "Undermine") // {U}{U}{B}: counter target spell
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 8) // {6}{R}{R}
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Obliterate")
                withClue("Obliterate should cast: ${cast.error}") { cast.error shouldBe null }
                game.execute(PassPriority(game.player1Id))

                val counter = game.castSpellTargetingStackSpell(2, "Undermine", "Obliterate")
                withClue("Undermine may still target an uncounterable spell: ${counter.error}") {
                    counter.error shouldBe null
                }
                game.resolveStack()

                withClue("Obliterate resolves despite Undermine — the creature is destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Obliterate is not countered; it resolves into its owner's graveyard") {
                    game.isInGraveyard(1, "Obliterate") shouldBe true
                }
            }
        }

        context("Rout") {
            test("destroys all creatures") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rout")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sparring Golem")
                    .withLandsOnBattlefield(1, "Plains", 5) // {3}{W}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Rout")
                game.resolveStack()

                withClue("Rout should destroy every creature") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Sparring Golem") shouldBe false
                }
                withClue("Lands should survive Rout") {
                    game.isOnBattlefield("Plains") shouldBe true
                }
            }

            test("flash-kicker mode lets it wipe the board on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rout")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 7) // {3}{W}{W} + {2} flash kicker
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val casterId = game.player1Id
                val routId = game.state.getHand(casterId).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Rout"
                }
                val cast = game.execute(CastSpell(playerId = casterId, cardId = routId, wasKicked = true))
                withClue("Flash-kicker cast on opponent's turn should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Rout cast for its flash kicker still destroys every creature") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("sorcery-speed cast on the opponent's turn is rejected without the kicker") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rout")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val casterId = game.player1Id
                val routId = game.state.getHand(casterId).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Rout"
                }
                val cast = game.execute(CastSpell(playerId = casterId, cardId = routId, wasKicked = false))
                withClue("Sorcery-speed Rout on the opponent's turn must be blocked") {
                    cast.error shouldNotBe null
                }
            }
        }

        context("Winnow") {
            test("destroys the target when another permanent shares its name") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Winnow")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears") // duplicate name on battlefield
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 2) // {1}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetBear = game.findPermanents("Grizzly Bears").first()
                val handBefore = game.handSize(1)
                game.castSpell(1, "Winnow", targetBear)
                game.resolveStack()

                withClue("One Grizzly Bears should be destroyed, the other remains") {
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
                withClue("Winnow always draws a card (casting spends it, the draw replaces it)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("does not destroy the target when no other permanent shares its name, but still draws") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Winnow")
                    .withCardOnBattlefield(2, "Grizzly Bears") // lone copy
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 2) // {1}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetBear = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)
                game.castSpell(1, "Winnow", targetBear)
                game.resolveStack()

                withClue("Lone Grizzly Bears should survive (no same-named permanent)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Winnow still draws a card even when nothing is destroyed") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
