package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.ForbiddenOrchard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Forbidden Orchard — "{T}: Add one mana of any color. Whenever you tap this land for mana, target
 * opponent creates a 1/1 colorless Spirit creature token."
 *
 * Exercises `Triggers.self.tappedForMana()`: the rider fires on a manual activation and when the
 * auto-payer taps the land for a spell, uses the stack (it targets, so it isn't a mana ability), and
 * does not fire when the land is tapped by anything other than its mana ability.
 */
class ForbiddenOrchardScenarioTest : FunSpec({

    val manaAbilityId = ForbiddenOrchard.activatedAbilities.single().id

    val TestBear = card("Orchard Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val TapSpell = card("Orchard Test Tap") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Land)
            effect = Effects.Tap(t)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ForbiddenOrchard, TestBear, TapSpell))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    fun GameTestDriver.spiritsOf(playerId: EntityId): List<EntityId> =
        getCreatures(playerId).filter { id ->
            state.getEntity(id)!!.has<TokenComponent>() && "Spirit" in state.projectedState.getSubtypes(id)
        }

    /** Answer the trigger's "target opponent" prompt if the engine asks rather than auto-picking. */
    fun GameTestDriver.targetOpponentIfAsked(you: EntityId, opponent: EntityId) {
        if (pendingDecision is ChooseTargetsDecision) {
            submitTargetSelection(you, listOf(opponent)).error shouldBe null
        }
    }

    test("tapping it for mana adds the chosen color and gives the opponent a 1/1 colorless Spirit") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val orchard = d.putPermanentOnBattlefield(you, "Forbidden Orchard")

        d.submit(
            ActivateAbility(playerId = you, sourceId = orchard, abilityId = manaAbilityId, manaColorChoice = Color.GREEN)
        ).error shouldBe null
        d.targetOpponentIfAsked(you, opponent)

        d.state.getEntity(you)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        // The rider is on the stack, not resolved yet — it is not a mana ability.
        d.stackSize shouldBe 1
        d.spiritsOf(opponent).shouldBeEmpty()

        d.bothPass()

        val spirits = d.spiritsOf(opponent)
        spirits.size shouldBe 1
        val spirit = spirits.single()
        d.state.projectedState.getPower(spirit) shouldBe 1
        d.state.projectedState.getToughness(spirit) shouldBe 1
        d.state.projectedState.getColors(spirit) shouldBe emptySet()
        d.spiritsOf(you).shouldBeEmpty()
    }

    test("the auto-payer tapping it for a spell fires the trigger too") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(you, "Forbidden Orchard")

        val bear = d.putCardInHand(you, "Orchard Test Bear")
        d.submit(CastSpell(playerId = you, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null
        d.targetOpponentIfAsked(you, opponent)

        // The bear and the Orchard's trigger (on top) are both on the stack.
        d.stackSize shouldBe 2
        d.bothPass()
        d.spiritsOf(opponent).size shouldBe 1
        d.bothPass()
        d.findPermanent(you, "Orchard Test Bear") shouldBe bear
    }

    test("being tapped by a spell is not tapping it for mana") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val orchard = d.putPermanentOnBattlefield(you, "Forbidden Orchard")

        d.giveMana(you, Color.BLUE, 1)
        val tapSpell = d.putCardInHand(you, "Orchard Test Tap")
        d.submit(
            CastSpell(
                playerId = you,
                cardId = tapSpell,
                targets = listOf(ChosenTarget.Permanent(orchard)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        d.bothPass()

        d.isTapped(orchard) shouldBe true
        d.stackSize shouldBe 0
        d.spiritsOf(opponent).shouldBeEmpty()
    }

    test("an opponent tapping their own Orchard gives you the Spirit") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val theirOrchard = d.putPermanentOnBattlefield(opponent, "Forbidden Orchard")
        d.passPriority(you)

        d.submit(
            ActivateAbility(playerId = opponent, sourceId = theirOrchard, abilityId = manaAbilityId, manaColorChoice = Color.RED)
        ).error shouldBe null
        d.targetOpponentIfAsked(opponent, you)
        d.stackSize shouldBe 1
        d.bothPass()

        d.spiritsOf(you).size shouldBe 1
        d.spiritsOf(opponent).shouldBeEmpty()
    }
})
