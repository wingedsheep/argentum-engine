package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.LilianaHereticalHealer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Liliana, Heretical Healer // Liliana, Defiant Necromancer (ORI #106).
 *
 * Front: "Whenever another nontoken creature you control dies, exile Liliana, then return her to
 * the battlefield transformed under her owner's control. If you do, create a 2/2 black Zombie
 * creature token." Ruling (2015-06-22): when several die at once only the first trigger to
 * resolve flips her and makes a Zombie; the rest do nothing.
 * Back: +2 has each player discard a card — you choose first, then each other player in turn
 * order without seeing your choice, then all are discarded at once (ruling 2015-06-22); −X returns a nonlegendary creature card with mana value exactly X; the −8 emblem returns
 * every creature that dies under your control at the beginning of the next end step.
 */
class LilianaHereticalHealerScenarioTest : FunSpec({
    val nonlegendaryWrath = card("Liliana Test Nonlegendary Wrath") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature.nonlegendary()) }
    }
    val wrath = card("Liliana Test Wrath") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature) }
    }

    val back = LilianaHereticalHealer.backFace!!.script
    val plusTwo = back.activatedAbilities[0]
    val minusX = back.activatedAbilities[1]
    val minusEight = back.activatedAbilities[2]

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(LilianaHereticalHealer, nonlegendaryWrath, wrath))
        it.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 40) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.bolt(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, listOf(target)).error shouldBe null
        settle()
    }

    fun GameTestDriver.castSorcery(name: String) {
        val spell = putCardInHand(player1, name)
        giveMana(player1, Color.BLACK, 1)
        castSpell(player1, spell).error shouldBe null
        settle()
    }

    fun GameTestDriver.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    fun GameTestDriver.zombieTokens(): List<EntityId> =
        getCreatures(player1).filter { state.getEntity(it)!!.has<TokenComponent>() }

    /** Liliana flipped by a Bolted Grizzly Bears; returns the planeswalker and the dead Bears. */
    fun GameTestDriver.flipLiliana(): Pair<EntityId, EntityId> {
        putCreatureOnBattlefield(player1, "Liliana, Heretical Healer")
        val bears = putCreatureOnBattlefield(player1, "Grizzly Bears")
        bolt(bears)
        val necromancer = findPermanent(player1, "Liliana, Defiant Necromancer").shouldNotBeNull()
        return necromancer to bears
    }

    test("another nontoken creature dying flips Liliana into a 3-loyalty walker and makes a Zombie") {
        val d = driver()
        val (necromancer, bears) = d.flipLiliana()

        d.getGraveyard(d.player1) shouldContain bears
        d.findPermanent(d.player1, "Liliana, Heretical Healer") shouldBe null
        d.loyalty(necromancer) shouldBe 3
        d.state.projectedState.isCreature(necromancer) shouldBe false
        d.zombieTokens().size shouldBe 1
    }

    test("two creatures dying at once flip her once and make only one Zombie") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Liliana, Heretical Healer")
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.castSorcery("Liliana Test Nonlegendary Wrath")

        val necromancer = d.findPermanent(d.player1, "Liliana, Defiant Necromancer").shouldNotBeNull()
        withClue("the second trigger finds a new object and does nothing") {
            d.zombieTokens().size shouldBe 1
            d.loyalty(necromancer) shouldBe 3
        }
    }

    test("Liliana dying alongside the creature stays in the graveyard and makes no Zombie") {
        val d = driver()
        val liliana = d.putCreatureOnBattlefield(d.player1, "Liliana, Heretical Healer")
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.castSorcery("Liliana Test Wrath")

        d.getGraveyard(d.player1) shouldContain liliana
        d.findPermanent(d.player1, "Liliana, Defiant Necromancer") shouldBe null
        d.zombieTokens().size shouldBe 0
    }

    test("a token creature dying doesn't flip her") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Liliana, Heretical Healer")
        val token = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also { d.addComponent(it, TokenComponent) }

        d.bolt(token)

        d.findPermanent(d.player1, "Liliana, Heretical Healer").shouldNotBeNull()
        d.findPermanent(d.player1, "Liliana, Defiant Necromancer") shouldBe null
    }

    test("+2: you choose first, the opponent chooses without your card having moved, then both discard") {
        val d = driver()
        val (necromancer, _) = d.flipLiliana()
        val yourHand = d.getHand(d.player1)
        val theirHand = d.getHand(d.player2)

        d.submitSuccess(ActivateAbility(d.player1, necromancer, plusTwo.id))
        d.loyalty(necromancer) shouldBe 5
        d.bothPass()

        val yours = d.pendingDecision as SelectCardsDecision
        yours.playerId shouldBe d.player1
        yours.options.toSet() shouldBe yourHand.toSet()
        val yourDiscard = yours.options.first()
        d.submitDecision(d.player1, CardsSelectedResponse(yours.id, listOf(yourDiscard))).error shouldBe null

        val theirs = d.pendingDecision as SelectCardsDecision
        theirs.playerId shouldBe d.player2
        theirs.options.toSet() shouldBe theirHand.toSet()
        withClue("your card is still in your hand while the opponent chooses") {
            d.getHand(d.player1) shouldContain yourDiscard
        }
        val theirDiscard = theirs.options.first()
        d.submitDecision(d.player2, CardsSelectedResponse(theirs.id, listOf(theirDiscard))).error shouldBe null

        d.getGraveyard(d.player1) shouldContain yourDiscard
        d.getGraveyard(d.player2) shouldContain theirDiscard
        d.getHand(d.player1) shouldNotContain yourDiscard
        d.getHand(d.player2) shouldNotContain theirDiscard
    }

    test("−X returns a nonlegendary creature card with mana value exactly X") {
        val d = driver()
        val (necromancer, bears) = d.flipLiliana()

        withClue("X = 1 can't target a mana value 2 card") {
            d.submitExpectFailure(
                ActivateAbility(
                    d.player1, necromancer, minusX.id,
                    targets = listOf(entityIdToChosenTarget(d.state, bears)), xValue = 1,
                )
            )
        }

        d.submitSuccess(
            ActivateAbility(
                d.player1, necromancer, minusX.id,
                targets = listOf(entityIdToChosenTarget(d.state, bears)), xValue = 2,
            )
        )
        d.loyalty(necromancer) shouldBe 1
        d.settle()

        d.state.getBattlefield() shouldContain bears
        d.getController(bears) shouldBe d.player1
    }

    test("−8 emblem: a creature that dies returns under your control at the next end step") {
        val d = driver()
        val (necromancer, ownBears) = d.flipLiliana()
        d.addComponent(necromancer, CountersComponent(mapOf(CounterType.LOYALTY to 8)))

        d.submitSuccess(ActivateAbility(d.player1, necromancer, minusEight.id))
        d.settle()

        val theirBears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.bolt(theirBears)
        d.getGraveyard(d.player2) shouldContain theirBears

        d.passPriorityUntil(Step.END)
        d.settle()

        withClue("the opponent's creature comes back under the emblem owner's control") {
            d.state.getBattlefield() shouldContain theirBears
            d.getController(theirBears) shouldBe d.player1
        }
        withClue("the creature that died before the emblem existed stays in the graveyard") {
            d.getGraveyard(d.player1) shouldContain ownBears
        }
    }
})
