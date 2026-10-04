package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ajani, Nacatl Pariah // Ajani, Nacatl Avenger (MH3 #237).
 *
 * Front: ETB makes a 2/1 Cat Warrior; whenever one or more other Cats you control die, you may
 * exile Ajani and return him transformed.
 * Back: +2 counters on each Cat; 0 makes a Cat Warrior and, with another red permanent, a reflexive
 * "deals damage equal to creatures you control to any target"; −4 each opponent keeps one artifact,
 * creature, enchantment and planeswalker and sacrifices the rest of their nonland permanents.
 */
class AjaniNacatlPariahScenarioTest : ScenarioTestBase() {

    private val front = "Ajani, Nacatl Pariah"
    private val back = "Ajani, Nacatl Avenger"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun abilityId(index: Int) = cardRegistry.requireCard(back).script.activatedAbilities[index].id

    /** Cat Warrior tokens [playerId] controls (every token these tests make is one). */
    private fun catWarriors(game: TestGame, playerId: EntityId): List<EntityId> =
        game.state.getBattlefield().filter {
            val entity = game.state.getEntity(it)
            entity?.has<TokenComponent>() == true &&
                entity.get<ControllerComponent>()?.playerId == playerId &&
                game.state.projectedState.hasSubtype(it, "Cat") &&
                game.state.projectedState.hasSubtype(it, "Warrior")
        }

    private fun avengerOnMyTurn(extra: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, back)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(extra)
        .build()

    init {
        test("casting Ajani creates a 2/1 white Cat Warrior token") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, front)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, front).error shouldBe null
            game.resolveStack()

            game.findPermanent(front) shouldNotBe null
            val tokens = catWarriors(game, game.player1Id)
            withClue("exactly one Cat Warrior token") { tokens.size shouldBe 1 }
            val token = tokens.single()
            game.state.projectedState.getPower(token) shouldBe 2
            game.state.projectedState.getToughness(token) shouldBe 1
        }

        test("another Cat dying lets you flip Ajani into a 3-loyalty planeswalker") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val lions = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Shock", lions).error shouldBe null
            game.resolveStack()
            withClue("the Cat-died trigger asks whether to flip") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true)
            game.resolveStack()

            game.findPermanent(front) shouldBe null
            val avenger = game.findPermanent(back)
            withClue("Ajani returned transformed") { avenger shouldNotBe null }
            loyalty(game, avenger!!) shouldBe 3
        }

        test("declining the flip keeps Ajani on his front face") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val lions = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Shock", lions).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.findPermanent(front) shouldNotBe null
            game.findPermanent(back) shouldBe null
        }

        test("a non-Cat dying doesn't trigger the flip") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Shock", bears).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.findPermanent(front) shouldNotBe null
        }

        test("+2 puts a +1/+1 counter on each Cat you control and nothing else") {
            val game = avengerOnMyTurn {
                withCardOnBattlefield(1, "Savannah Lions")
                withCardOnBattlefield(1, "Grizzly Bears")
                withCardOnBattlefield(2, "Savannah Lions")
            }
            val ajani = game.findPermanent(back)!!
            val myLions = game.findPermanents("Savannah Lions")
                .single { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id }
            val theirLions = game.findPermanents("Savannah Lions").single { it != myLions }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(ActivateAbility(game.player1Id, ajani, abilityId(0))).error shouldBe null
            game.resolveStack()

            loyalty(game, ajani) shouldBe 5
            plusOnes(game, myLions) shouldBe 1
            withClue("Grizzly Bears isn't a Cat") { plusOnes(game, bears) shouldBe 0 }
            withClue("the opponent's Cat isn't yours") { plusOnes(game, theirLions) shouldBe 0 }
        }

        test("0 with another red permanent: token, then damage equal to creatures you control") {
            val game = avengerOnMyTurn {
                withCardOnBattlefield(1, "Raging Goblin")
            }
            val ajani = game.findPermanent(back)!!

            game.execute(ActivateAbility(game.player1Id, ajani, abilityId(1))).error shouldBe null
            game.resolveStack()
            withClue("the reflexive trigger chooses its target as it goes on the stack") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            loyalty(game, ajani) shouldBe 3
            catWarriors(game, game.player1Id).size shouldBe 1
            withClue("Raging Goblin + the new token = 2 creatures") { game.getLifeTotal(2) shouldBe 18 }
        }

        test("0 without another red permanent just makes the token") {
            val game = avengerOnMyTurn {
                withCardOnBattlefield(1, "Savannah Lions")
            }
            val ajani = game.findPermanent(back)!!

            game.execute(ActivateAbility(game.player1Id, ajani, abilityId(1))).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            catWarriors(game, game.player1Id).size shouldBe 1
            game.getLifeTotal(2) shouldBe 20
        }

        test("−4: the opponent keeps one of each listed type and sacrifices the rest; lands stay") {
            val game = avengerOnMyTurn {
                withCardOnBattlefield(2, "Grizzly Bears")
                withCardOnBattlefield(2, "Savannah Lions")
                withCardOnBattlefield(2, "Ornithopter")
                withLandsOnBattlefield(2, "Forest", 1)
            }
            val ajani = game.findPermanent(back)!!
            game.state = game.state.updateEntity(ajani) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
            }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(ActivateAbility(game.player1Id, ajani, abilityId(2))).error shouldBe null
            game.resolveStack()
            var guard = 0
            while (guard++ < 10) {
                val decision = game.getPendingDecision() as? SelectCardsDecision ?: break
                decision.playerId shouldBe game.player2Id
                game.selectCards(listOf(if (bears in decision.options) bears else decision.options.first()))
                game.resolveStack()
            }

            withClue("the chosen creature survives") { game.findPermanent("Grizzly Bears") shouldNotBe null }
            withClue("Ornithopter is the only artifact, so it's kept") {
                game.findPermanent("Ornithopter") shouldNotBe null
            }
            withClue("the unchosen creature is sacrificed") {
                game.isInGraveyard(2, "Savannah Lions") shouldBe true
            }
            withClue("lands are never sacrificed") { game.findPermanent("Forest") shouldNotBe null }
            withClue("Ajani at 0 loyalty is put into the graveyard") { game.findPermanent(back) shouldBe null }
        }
    }
}
