package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.LilianaDreadhordeGeneral
import com.wingedsheep.mtg.sets.definitions.war.cards.NarsetParterOfVeils
import com.wingedsheep.mtg.sets.definitions.war.cards.NicolBolasDragonGod
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nicol Bolas, Dragon-God (WAR #207; {U}{B}{B}{B}{R}, Loyalty 4).
 *
 *   Nicol Bolas has all loyalty abilities of all other planeswalkers on the battlefield.
 *   +1: You draw a card. Each opponent exiles a card from their hand or a permanent they control.
 *   −3: Destroy target creature or planeswalker.
 *   −8: Each opponent who doesn't control a legendary creature or planeswalker loses the game.
 *
 * The borrowed abilities are `GainActivatedAbilitiesOfPermanents(abilities = LOYALTY)`: copies
 * sourced from Bolas, so they spend Bolas's loyalty and share his one-per-turn limit (CR 606.3).
 */
class NicolBolasDragonGodScenarioTest : ScenarioTestBase() {

    private val bolasPlusOne = NicolBolasDragonGod.activatedAbilities[0].id
    private val bolasMinusEight = NicolBolasDragonGod.activatedAbilities[2].id
    private val lilianaPlusOne = LilianaDreadhordeGeneral.activatedAbilities[0].id
    private val narsetMinusTwo = NarsetParterOfVeils.activatedAbilities[0].id

    private fun TestGame.setLoyalty(id: EntityId, loyalty: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) }
    }

    private fun TestGame.loyalty(id: EntityId): Int? =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY)

    init {
        context("Nicol Bolas, Dragon-God") {

            test("activates another planeswalker's loyalty ability, paying with his own loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(2, "Liliana, Dreadhorde General")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                val liliana = game.findPermanent("Liliana, Dreadhorde General")!!
                game.setLoyalty(bolas, 4)
                game.setLoyalty(liliana, 6)

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = lilianaPlusOne))
                    .error shouldBe null
                game.resolveStack()

                withClue("Bolas's controller gets the Zombie") {
                    game.state.getBattlefield(game.player1Id).count { id ->
                        game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Zombie Token"
                    } shouldBe 1
                }
                withClue("the +1 adds loyalty to Bolas, not to Liliana") {
                    game.loyalty(bolas) shouldBe 5
                    game.loyalty(liliana) shouldBe 6
                }
            }

            test("a borrowed loyalty ability counts toward Bolas's one loyalty activation per turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(2, "Liliana, Dreadhorde General")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                game.setLoyalty(bolas, 4)

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = lilianaPlusOne))
                    .error shouldBe null
                game.resolveStack()

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = bolasPlusOne))
                    .error shouldNotBe null
            }

            test("copies only loyalty abilities, and loses them when that planeswalker leaves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(2, "Narset, Parter of Veils")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                val narset = game.findPermanent("Narset, Parter of Veils")!!
                game.setLoyalty(bolas, 4)
                game.setLoyalty(narset, 2)

                game.castSpell(1, "Lightning Bolt", narset).error shouldBe null
                game.resolveStack()
                game.findPermanent("Narset, Parter of Veils") shouldBe null

                withClue("Narset's −2 is gone from Bolas once Narset is") {
                    game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = narsetMinusTwo))
                        .error shouldNotBe null
                }
            }

            test("+1: you draw, then the opponent exiles a permanent they control or a card in hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                game.setLoyalty(bolas, 4)
                val bears = game.findPermanent("Grizzly Bears")!!
                val bolt = game.findCardsInHand(2, "Lightning Bolt").single()

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = bolasPlusOne))
                    .error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                val choice = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the opponent's exile choice; got ${game.getPendingDecision()}")
                choice.playerId shouldBe game.player2Id
                withClue("the pool is the opponent's hand plus their permanents") {
                    choice.options.toSet() shouldBe setOf(bears, bolt)
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInHand(2, "Lightning Bolt") shouldBe true
                game.loyalty(bolas) shouldBe 5
            }

            test("+1 offers a permanent the opponent controls but doesn't own, not one they own but don't control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                game.setLoyalty(bolas, 4)
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                // Swap control: the opponent now controls our Bears, and we control their Giant.
                game.state = game.state
                    .updateEntity(bears) { c -> c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(game.player2Id)) }
                    .updateEntity(giant) { c -> c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(game.player1Id)) }

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = bolasPlusOne))
                    .error shouldBe null
                game.resolveStack()

                withClue("the Bears are the opponent's only option, so they're exiled without a prompt") {
                    game.getPendingDecision() shouldBe null
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
                withClue("the Giant the opponent owns but we control stays") {
                    game.findPermanent("Hill Giant") shouldBe giant
                }
            }

            test("−8: an opponent without a legendary creature or planeswalker loses") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                game.setLoyalty(bolas, 8)

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = bolasMinusEight))
                    .error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe true
            }

            test("−8: an opponent with a legendary creature survives") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nicol Bolas, Dragon-God")
                    .withCardOnBattlefield(2, "Isamaru, Hound of Konda")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bolas = game.findPermanent("Nicol Bolas, Dragon-God")!!
                game.setLoyalty(bolas, 8)

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bolas, abilityId = bolasMinusEight))
                    .error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe false
            }
        }
    }
}
