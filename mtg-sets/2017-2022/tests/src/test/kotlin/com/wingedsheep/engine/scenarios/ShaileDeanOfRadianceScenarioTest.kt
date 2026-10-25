package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.stx.cards.ShaileDeanOfRadiance
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shaile, Dean of Radiance // Embrose, Dean of Shadow (STX #158).
 *
 * Shaile's {T} counts entries, not current control: a creature that entered under your control
 * this turn gets a counter even after an opponent takes it (ruling), one that was already on the
 * battlefield doesn't, and one that entered under an opponent's control doesn't even once you
 * control it. Embrose's {T} puts the counter on first, then deals 2 — and a creature you control
 * that dies with that counter on it draws you a card.
 */
class ShaileDeanOfRadianceScenarioTest : ScenarioTestBase() {

    private val bear = card("Shaile Test Bear") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val veteran = card("Shaile Test Veteran") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }
    private val squire = card("Shaile Test Squire") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    private val giftOfSoldiers = card("Shaile Test Gift") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Soldier"),
                controller = EffectTarget.PlayerRef(Player.EachOpponent),
            )
        }
    }

    init {
        listOf(bear, veteran, squire, giftOfSoldiers).forEach { cardRegistry.register(it) }

        fun TestGame.plusOneCounters(id: EntityId): Int =
            state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun TestGame.giveControl(id: EntityId, to: EntityId) {
            state = state.updateEntity(id) { it.with(ControllerComponent(to)) }
        }

        fun TestGame.activateShaile() {
            val shaile = findPermanent("Shaile, Dean of Radiance")!!
            val abilityId = ShaileDeanOfRadiance.activatedAbilities[0].id
            execute(ActivateAbility(player1Id, shaile, abilityId)).error shouldBe null
            resolveStack()
        }

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Shaile, Dean of Radiance")
            .withCardOnBattlefield(1, "Shaile Test Veteran")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardInHand(1, "Shaile Test Bear")
            .withCardInHand(1, "Shaile Test Gift")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Shaile — {T}: a counter on each creature that entered under your control this turn") {

            test("only the creature that entered this turn gets a counter") {
                val game = board()
                game.castSpell(1, "Shaile Test Bear").error shouldBe null
                game.resolveStack()

                game.activateShaile()

                withClue("the bear entered under your control this turn") {
                    game.plusOneCounters(game.findPermanent("Shaile Test Bear")!!) shouldBe 1
                }
                withClue("the veteran was already on the battlefield") {
                    game.plusOneCounters(game.findPermanent("Shaile Test Veteran")!!) shouldBe 0
                }
                withClue("Shaile was already on the battlefield") {
                    game.plusOneCounters(game.findPermanent("Shaile, Dean of Radiance")!!) shouldBe 0
                }
            }

            test("a creature flickered after it entered is a new object that entered once more — one counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shaile, Dean of Radiance")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(1, "Shaile Test Bear")
                    .withCardInHand(1, "Cloudshift")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Shaile Test Bear").error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Cloudshift", game.findPermanent("Shaile Test Bear")!!).error shouldBe null
                game.resolveStack()

                game.activateShaile()

                game.plusOneCounters(game.findPermanent("Shaile Test Bear")!!) shouldBe 1
            }

            test("a creature that entered under your control still gets one after an opponent takes it") {
                val game = board()
                game.castSpell(1, "Shaile Test Bear").error shouldBe null
                game.resolveStack()
                val bearId = game.findPermanent("Shaile Test Bear")!!
                game.giveControl(bearId, game.player2Id)

                game.activateShaile()

                game.plusOneCounters(bearId) shouldBe 1
            }

            test("a creature that entered under an opponent's control doesn't, even once you control it") {
                val game = board()
                game.castSpell(1, "Shaile Test Gift").error shouldBe null
                game.resolveStack()
                val soldier = game.state.getBattlefield().single { id ->
                    game.state.getEntity(id)!!.get<CardComponent>()!!.name.contains("Soldier") &&
                        game.state.getEntity(id)!!.get<ControllerComponent>()!!.playerId == game.player2Id
                }
                game.giveControl(soldier, game.player1Id)

                game.activateShaile()

                game.plusOneCounters(soldier) shouldBe 0
            }
        }

        context("Embrose — {T}: counter, then 2 damage, to another target creature") {

            fun embroseBoard() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Embrose, Dean of Shadow")
                .withCardOnBattlefield(1, "Shaile Test Bear")
                .withCardOnBattlefield(1, "Shaile Test Squire")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.activateEmbrose(target: EntityId) {
                val embrose = findPermanent("Embrose, Dean of Shadow")!!
                val abilityId = ShaileDeanOfRadiance.backFace!!.activatedAbilities[0].id
                execute(
                    ActivateAbility(player1Id, embrose, abilityId, targets = listOf(ChosenTarget.Permanent(target)))
                ).error shouldBe null
                resolveStack()
            }

            test("the target gets a +1/+1 counter and 2 damage, so a 2/2 survives as a 3/3") {
                val game = embroseBoard()
                val bearId = game.findPermanent("Shaile Test Bear")!!

                game.activateEmbrose(bearId)

                game.findPermanent("Shaile Test Bear") shouldBe bearId
                game.plusOneCounters(bearId) shouldBe 1
                withClue("nothing died, so nothing was drawn") {
                    game.state.getHand(game.player1Id).size shouldBe 0
                }
            }

            test("a creature you control dying with a +1/+1 counter on it draws a card") {
                val game = embroseBoard()

                game.activateEmbrose(game.findPermanent("Shaile Test Squire")!!)

                game.findPermanent("Shaile Test Squire") shouldBe null
                game.state.getHand(game.player1Id).size shouldBe 1
            }
        }
    }
}
