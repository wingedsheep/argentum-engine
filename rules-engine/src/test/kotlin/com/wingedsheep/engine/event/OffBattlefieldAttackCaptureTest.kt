package com.wingedsheep.engine.event

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever you attack with one or more creatures, create **that many** …" detected off the
 * battlefield scan — as a delayed trigger ("… this turn", Basri Ket's −2) and as an emblem — freezes
 * the attackers it counted at declaration, exactly like the battlefield-resident form (Rabble
 * Rousing). So the count is the declared matching attackers (CR 508.4: only declared attackers),
 * and an attacker that leaves combat before the trigger resolves still counts.
 */
class OffBattlefieldAttackCaptureTest : FunSpec({

    val thatMany = Effects.CreateToken(
        count = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured),
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE),
        creatureTypes = setOf("Soldier")
    )

    val delayedSorcery: CardDefinition = card("Test Delayed Muster") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Whenever one or more nontoken creatures attack this turn, create that many 1/1 white Soldier creature tokens."
        spell {
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.anyPlayer.attacks(with = GameObjectFilter.Creature.nontoken()),
                effect = thatMany
            )
        }
    }

    val emblemSorcery: CardDefinition = card("Test Muster Emblem") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "You get an emblem with \"Whenever you attack with one or more creatures, create that many 1/1 white Soldier creature tokens.\""
        spell {
            effect = Effects.CreateGlobalTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.you.attacks()
                    effect = thatMany
                    description = "Whenever you attack with one or more creatures, create that many 1/1 white Soldier creature tokens."
                },
                emblem = true
            )
        }
    }

    val bounce: CardDefinition = card("Test Bounce") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Return target creature to its owner's hand."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(creature)
        }
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(delayedSorcery, emblemSorcery, bounce))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll() {
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    fun GameTestDriver.castAndResolve(name: String) {
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, putCardInHand(player1, name)).error shouldBe null
        resolveAll()
    }

    fun GameTestDriver.attackers(count: Int): List<EntityId> = List(count) {
        putCreatureOnBattlefield(player1, "Centaur Courser").also { removeSummoningSickness(it) }
    }

    fun GameTestDriver.soldiers(): Int = getCreatures(player1).count {
        state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Soldier Token"
    }

    /** Declare [attackers], bounce the first one with the trigger on the stack, then resolve. */
    fun GameTestDriver.attackAndBounceOne(attackers: List<EntityId>) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers, player2).error shouldBe null
        stackSize shouldBe 1
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, putCardInHand(player1, "Test Bounce"), listOf(attackers.first())).error shouldBe null
        resolveAll()
    }

    test("delayed trigger counts the declared nontoken attackers, including one bounced in response") {
        val game = newGame()
        val attackers = game.attackers(3)
        game.castAndResolve("Test Delayed Muster")

        game.attackAndBounceOne(attackers)

        game.soldiers() shouldBe 3
    }

    test("emblem trigger counts the declared attackers, including one bounced in response") {
        val game = newGame()
        val attackers = game.attackers(2)
        game.castAndResolve("Test Muster Emblem")

        game.attackAndBounceOne(attackers)

        game.soldiers() shouldBe 2
    }
})
