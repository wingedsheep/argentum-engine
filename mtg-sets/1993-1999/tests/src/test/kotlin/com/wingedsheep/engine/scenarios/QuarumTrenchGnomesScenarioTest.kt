package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Quarum Trench Gnomes (LEG #162).
 *
 * {3}{R} Creature — Gnome 1/1
 * "{T}: If target Plains is tapped for mana, it produces colorless mana instead of white mana.
 *  (This effect lasts indefinitely.)"
 *
 * Pins the three things the rule says: only the *targeted* Plains changes, only its *white* mana
 * becomes colorless, and the effect is indefinite — it outlives the Gnomes. The auto-pay solver
 * must see the same thing a manual tap does, so a {W} spell can't be paid off that Plains.
 */
class QuarumTrenchGnomesScenarioTest : ScenarioTestBase() {

    init {
        fun ScenarioTestBase.TestGame.manaPool(): ManaPoolComponent =
            state.getEntity(player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

        fun ScenarioTestBase.TestGame.tapForMana(landId: EntityId, landName: String) {
            val manaAbility = cardRegistry.getCard(landName)!!.script.activatedAbilities[0].id
            execute(ActivateAbility(playerId = player1Id, sourceId = landId, abilityId = manaAbility))
                .error shouldBe null
        }

        fun ScenarioTestBase.TestGame.activateGnomesOn(plains: EntityId) {
            val gnomes = findPermanent("Quarum Trench Gnomes")!!
            val abilityId = cardRegistry.getCard("Quarum Trench Gnomes")!!.script.activatedAbilities[0].id
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = gnomes,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(plains)),
                )
            ).error shouldBe null
            resolveStack()
        }

        context("Quarum Trench Gnomes") {

            test("the targeted Plains taps for colorless; another Plains still taps for white") {
                val game = scenario()
                    .withPlayers("Gnomes", "Opponent")
                    .withCardOnBattlefield(1, "Quarum Trench Gnomes")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (target, other) = game.findPermanents("Plains")
                game.activateGnomesOn(target)

                game.tapForMana(target, "Plains")
                withClue("the targeted Plains produced {C} instead of {W}") {
                    game.manaPool().colorless shouldBe 1
                    game.manaPool().getAmount(Color.WHITE) shouldBe 0
                }

                game.tapForMana(other, "Plains")
                withClue("the untargeted Plains is unaffected") {
                    game.manaPool().getAmount(Color.WHITE) shouldBe 1
                }
            }

            test("only white mana is replaced: a Savannah still taps for green") {
                // The card's ruling: only white mana becomes colorless. Savannah (Forest Plains)
                // is a legal target and keeps its {G}.
                fun savannahGame() = scenario()
                    .withPlayers("Gnomes", "Opponent")
                    .withCardOnBattlefield(1, "Quarum Trench Gnomes")
                    .withCardOnBattlefield(1, "Savannah")
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                    .also { it.activateGnomesOn(it.findPermanent("Savannah")!!) }

                fun ScenarioTestBase.TestGame.tapSavannahFor(color: Char) {
                    execute(
                        ActivateAbility(
                            playerId = player1Id,
                            sourceId = findPermanent("Savannah")!!,
                            abilityId = AbilityId.intrinsicMana(color),
                        )
                    ).error shouldBe null
                }

                val white = savannahGame()
                white.tapSavannahFor('W')
                withClue("its {W} ability produced {C}") {
                    white.manaPool().colorless shouldBe 1
                    white.manaPool().getAmount(Color.WHITE) shouldBe 0
                }

                val green = savannahGame()
                green.tapSavannahFor('G')
                withClue("its {G} ability is untouched") {
                    green.manaPool().getAmount(Color.GREEN) shouldBe 1
                    green.manaPool().colorless shouldBe 0
                }

                val autoPay = savannahGame()
                withClue("auto-pay sees a green-or-colorless source: no {W} spell, but a {G} one") {
                    autoPay.castSpell(1, "Savannah Lions").error shouldNotBe null
                    autoPay.castSpell(1, "Llanowar Elves").error shouldBe null
                }
            }

            test("under Pulse of Llanowar the affected Plains still taps for any colour, on both paths") {
                // Pulse is a free-choice replacement and applies after the Gnomes' fixed one, so the
                // land can still make {W}; manual tap and auto-pay must agree on that.
                fun pulseGame() = scenario()
                    .withPlayers("Gnomes", "Opponent")
                    .withCardOnBattlefield(1, "Pulse of Llanowar")
                    .withCardOnBattlefield(1, "Quarum Trench Gnomes")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                    .also { it.activateGnomesOn(it.findPermanent("Plains")!!) }

                val manual = pulseGame()
                manual.execute(
                    ActivateAbility(
                        playerId = manual.player1Id,
                        sourceId = manual.findPermanent("Plains")!!,
                        abilityId = cardRegistry.getCard("Plains")!!.script.activatedAbilities[0].id,
                        manaColorChoice = Color.WHITE,
                    )
                ).error shouldBe null
                withClue("Pulse's choice is offered last, so white is still available") {
                    manual.manaPool().getAmount(Color.WHITE) shouldBe 1
                    manual.manaPool().colorless shouldBe 0
                }

                withClue("auto-pay agrees: the Plains can pay {W}") {
                    pulseGame().castSpell(1, "Savannah Lions").error shouldBe null
                }
            }

            test("the effect lasts after the Gnomes leave the battlefield") {
                val game = scenario()
                    .withPlayers("Gnomes", "Opponent")
                    .withCardOnBattlefield(1, "Quarum Trench Gnomes")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val plains = game.findPermanent("Plains")!!
                game.activateGnomesOn(plains)

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Quarum Trench Gnomes")).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Quarum Trench Gnomes") shouldBe false

                game.tapForMana(plains, "Plains")
                withClue("the indefinite effect outlives its source") {
                    game.manaPool().colorless shouldBe 1
                    game.manaPool().getAmount(Color.WHITE) shouldBe 0
                }
            }

            test("auto-pay can't take white mana from the affected Plains, but can take generic") {
                val game = scenario()
                    .withPlayers("Gnomes", "Opponent")
                    .withCardOnBattlefield(1, "Quarum Trench Gnomes")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateGnomesOn(game.findPermanent("Plains")!!)

                withClue("the Plains no longer makes {W}, so Savannah Lions can't be paid for") {
                    game.castSpell(1, "Savannah Lions").error shouldNotBe null
                }
                withClue("its colorless mana still pays a generic cost") {
                    game.castSpell(1, "Sol Ring").error shouldBe null
                }
            }
        }
    }
}
