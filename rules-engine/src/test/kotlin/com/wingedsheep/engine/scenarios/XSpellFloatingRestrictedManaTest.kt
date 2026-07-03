package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Casting an X spell when part of the payment is restricted mana ("Spend this mana only to cast
 * instant and sorcery spells") — floating in the pool or producible by granted mana abilities.
 *
 * Repro board (user report): Resonating Lute grants each land "{T}: Add two mana of any one
 * color. Spend this mana only to cast instant and sorcery spells." With 2 Islands, 1 Mountain,
 * an untapped Potioner's Trove ({T}: add one mana of any color), and {U}{R}{R}{R} already
 * floating, Procrastinate ({X}{U}, sorcery) must be castable with X = 10:
 *
 *   floating 4 + (3 lands × 2 restricted) + 1 trove = 11 mana; fixed cost {U} leaves X = 10.
 *
 * Bugs pinned here:
 *  1. `maxAffordableX` must be 10 both before and after floating the restricted mana into the
 *     pool (the count used to ignore floating restricted-mana entries → showed 4).
 *  2. Validation (`ManaSolver.canPay`) and payment must accept eligible restricted floating
 *     mana for the {X} portion.
 *  3. Restricted mana that is NOT eligible for the spell (a creature spell vs
 *     instant-or-sorcery-only mana) must not inflate `maxAffordableX`.
 */
class XSpellFloatingRestrictedManaTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun buildGame(handCard: String = "Procrastinate"): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, handCard)
        .withCardOnBattlefield(1, "Resonating Lute")
        .withCardOnBattlefield(1, "Potioner's Trove")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Give player 1 the reported floating mana: {U}{R}{R}{R}, unrestricted. */
    private fun floatBaseMana(game: TestGame) {
        game.state = game.state.updateEntity(game.player1Id) { c ->
            c.with(ManaPoolComponent(blue = 1, red = 3))
        }
    }

    private fun maxAffordableX(game: TestGame, spellName: String): Int? =
        game.getLegalActions(1)
            .first { info ->
                val cast = info.action as? CastSpell ?: return@first false
                game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == spellName
            }
            .maxAffordableX

    /** Activate the Lute-granted "{T}: Add two mana of any one color" ability on a land. */
    private fun activateGrantedLandAbility(game: TestGame, landId: EntityId, color: Color) {
        val utils = CastPermissionUtils(cardRegistry, PredicateEvaluator(), ConditionEvaluator())
        val grants = utils.getStaticGrantedAbilitiesWithGranter(landId, game.state)
        grants.size shouldBe 1
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = landId,
                abilityId = grants[0].ability.id,
                manaColorChoice = color,
            )
        ).error shouldBe null
    }

    /** Activate Potioner's Trove's "{T}: Add one mana of any color". */
    private fun activateTrove(game: TestGame, color: Color) {
        val trove = game.findPermanent("Potioner's Trove")!!
        val abilityId = cardRegistry.requireCard("Potioner's Trove").activatedAbilities[0].id
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = trove,
                abilityId = abilityId,
                manaColorChoice = color,
            )
        ).error shouldBe null
    }

    init {
        context("X spell with restricted-mana sources (Resonating Lute board)") {

            test("maxAffordableX is 10 with untapped sources and {U}{R}{R}{R} floating") {
                val game = buildGame()
                floatBaseMana(game)

                withClue("4 floating + 3 lands x 2 (granted, restricted) + 1 trove = 11; {U} fixed leaves X=10") {
                    maxAffordableX(game, "Procrastinate") shouldBe 10
                }
            }

            test("casting with X=10 via auto-pay succeeds and puts 20 stun counters") {
                val game = buildGame()
                floatBaseMana(game)
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.castXSpell(1, "Procrastinate", xValue = 10, targetId = bears)
                withClue("auto-pay should tap the 3 lands (via the granted 2-mana ability) + trove") {
                    result.error shouldBe null
                }
                game.resolveStack()

                stunCounters(game, bears) shouldBe 20
                withClue("all payment mana consumed") {
                    val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                    pool.total shouldBe 0
                }
            }

            test("explicit source selection with floating mana covering part of X succeeds") {
                // The client's manual mana-selection path submits PaymentStrategy.Explicit with
                // the chosen sources. The chosen sources can only produce 7 of the X=10; the
                // other 3 (plus the {U}) come from the floating pool. Validation must apply the
                // floating mana to X exactly like execution (explicitPay → autoPay) does.
                val game = buildGame()
                floatBaseMana(game)
                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Procrastinate"
                }
                val sources = game.findPermanents("Island") +
                    game.findPermanent("Mountain")!! +
                    game.findPermanent("Potioner's Trove")!!

                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        xValue = 10,
                        paymentStrategy = PaymentStrategy.Explicit(manaAbilitiesToActivate = sources),
                    )
                )
                result.error shouldBe null
                game.resolveStack()
                stunCounters(game, bears) shouldBe 20
            }

            test("maxAffordableX is still 10 after floating all mana into the pool") {
                val game = buildGame()
                floatBaseMana(game)

                // Manually tap everything, as the user did: each land via the granted
                // ability (2 restricted mana each), plus the trove (1 unrestricted).
                val islands = game.findPermanents("Island")
                islands.size shouldBe 2
                activateGrantedLandAbility(game, islands[0], Color.BLUE)
                activateGrantedLandAbility(game, islands[1], Color.BLUE)
                activateGrantedLandAbility(game, game.findPermanent("Mountain")!!, Color.RED)
                activateTrove(game, Color.BLUE)

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("2 unrestricted blue (1 floated + 1 trove) + 3 unrestricted red + 6 restricted") {
                    pool.blue shouldBe 2
                    pool.red shouldBe 3
                    pool.restrictedMana.size shouldBe 6
                }

                withClue("all 11 floating mana are spendable on a sorcery, so X can be 10") {
                    maxAffordableX(game, "Procrastinate") shouldBe 10
                }
            }

            test("casting with X=10 entirely from the floating pool succeeds") {
                val game = buildGame()
                floatBaseMana(game)
                val islands = game.findPermanents("Island")
                activateGrantedLandAbility(game, islands[0], Color.BLUE)
                activateGrantedLandAbility(game, islands[1], Color.BLUE)
                activateGrantedLandAbility(game, game.findPermanent("Mountain")!!, Color.RED)
                activateTrove(game, Color.BLUE)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castXSpell(1, "Procrastinate", xValue = 10, targetId = bears).error shouldBe null
                game.resolveStack()

                stunCounters(game, bears) shouldBe 20
                withClue("the whole pool (including restricted entries) is consumed") {
                    val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                    pool.total shouldBe 0
                    pool.restrictedMana.size shouldBe 0
                }
            }

            test("ineligible restricted mana does not inflate maxAffordableX for a creature spell") {
                // Mockingbird is {X}{U} — a creature spell, so instant-or-sorcery-only
                // restricted mana can't pay for it.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mockingbird")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.updateEntity(game.player1Id) { c ->
                    c.with(
                        ManaPoolComponent(
                            blue = 2,
                            restrictedMana = List(4) {
                                RestrictedManaEntry(Color.RED, ManaRestriction.InstantOrSorceryOnly)
                            },
                        )
                    )
                }

                withClue("only the 2 unrestricted blue count: {U} fixed leaves X=1") {
                    maxAffordableX(game, "Mockingbird") shouldBe 1
                }
            }
        }
    }
}
