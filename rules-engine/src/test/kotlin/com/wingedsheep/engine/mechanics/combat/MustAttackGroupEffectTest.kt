package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.handlers.effects.combat.MustAttackGroupExecutor
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

/**
 * [MustAttackGroupEffect] with [MustAttackDefender.PlayerOtherThanYou] — Kardur, Doomscourge's
 * "until your next turn, creatures your opponents control attack each combat if able and attack a
 * player other than you if able" — and the printed [MustAttack] static with the same defender.
 *
 * The rules pinned, from Kardur's rulings and CR 508.1d / 611.2c / 701.15b:
 *  - every creature the group covers must attack, including one that arrived after resolution;
 *  - while some other player could legally be attacked, attacking "you" is an error;
 *  - when no other player can be (a restriction, or two-player), the creature must still attack,
 *    and may attack "you";
 *  - the requirement ends at the start of its controller's next turn;
 *  - it stacks with goad: the attacked player must be named by no more requirements than another
 *    player the creature could attack.
 */
class MustAttackGroupEffectTest : FunSpec({

    val bear = card("Test Kardur Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    /** "Creatures can't attack you." */
    val ward = card("Test Kardur Ward") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
        oracleText = "Creatures can't attack you."
        staticAbility { ability = CantAttackYou(GroupFilter.AllCreatures) }
    }

    /** The printed form: "Creatures your opponents control attack each combat if able and attack a player other than you if able." */
    val banner = card("Test Kardur Banner") {
        manaCost = "{2}{R}"
        typeLine = "Enchantment"
        staticAbility {
            ability = MustAttack(GroupFilter.AllCreaturesOpponentsControl, MustAttackDefender.PlayerOtherThanYou)
        }
    }

    val kardurEffect = MustAttackGroupEffect(
        GroupFilter.AllCreaturesOpponentsControl,
        MustAttackDefender.PlayerOtherThanYou,
        Duration.UntilYourNextTurn
    )

    /** Seats P1..Pn; P2 takes the first turn so its creatures attack while P1's effect is live. */
    fun driverWith(seats: Int): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bear, ward, banner))
        val players = driver.initMultiplayer(decks = List(seats) { Deck.of("Forest" to 40) }, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun GameTestDriver.applyKardur(controller: EntityId) {
        val result = MustAttackGroupExecutor(PredicateEvaluator(cardRegistry = null).amounts).execute(state, kardurEffect, EffectContext(sourceId = null, controllerId = controller))
        replaceState(result.state)
    }

    fun GameTestDriver.readyBear(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Test Kardur Bear").also { removeSummoningSickness(it) }

    test("binds every creature opponents control, including one that arrives later, and none of yours") {
        val (driver, p) = driverWith(3)
        val mine = driver.readyBear(p[0])
        val theirs = driver.readyBear(p[1])
        driver.applyKardur(p[0])
        val later = driver.readyBear(p[2])

        val projected = driver.state.projectedState
        projected.mustAttack(theirs) shouldBe true
        projected.mustAttack(later) shouldBe true
        projected.mustAttack(mine) shouldBe false
        projected.mustAttackOtherThan(theirs) shouldBe listOf(p[0])
    }

    test("must attack, and must attack a player other than you while one can be attacked") {
        val (driver, p) = driverWith(3)
        val theirs = driver.readyBear(p[1])
        driver.applyKardur(p[0])
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.legalActions(p[1]).single { it.actionType == "DeclareAttackers" }
            .attackTargetsByAttacker?.get(theirs) shouldBe listOf(p[2])
        driver.declareAttackers(p[1], emptyMap()).error shouldNotBe null
        driver.declareAttackers(p[1], mapOf(theirs to p[0])).error!! shouldContain "a player other than"
        driver.declareAttackers(p[1], mapOf(theirs to p[2])).error shouldBe null
    }

    test("with no other player attackable, the creature still attacks and may attack you") {
        val (driver, p) = driverWith(3)
        val theirs = driver.readyBear(p[1])
        driver.putPermanentOnBattlefield(p[2], "Test Kardur Ward")
        driver.applyKardur(p[0])
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(p[1], emptyMap()).error shouldNotBe null
        driver.declareAttackers(p[1], mapOf(theirs to p[0])).error shouldBe null
    }

    test("two-player: the only opponent is you, so attacking you obeys it") {
        val (driver, p) = driverWith(2)
        val theirs = driver.readyBear(p[1])
        driver.applyKardur(p[0])
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(p[1], emptyMap()).error shouldNotBe null
        driver.declareAttackers(p[1], mapOf(theirs to p[0])).error shouldBe null
    }

    test("is a mandatory attacker for the declaration, and ends as its controller's next turn begins") {
        val (driver, p) = driverWith(2)
        val theirs = driver.readyBear(p[1])
        driver.applyKardur(p[0])
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.projectedState.mustAttack(theirs) shouldBe true

        driver.declareAttackers(p[1], mapOf(theirs to p[0])).error shouldBe null
        driver.passPriorityUntil(Step.UPKEEP, p[0])
        driver.state.projectedState.mustAttack(theirs) shouldBe false
    }

    test("stacks with goad: the player named by fewer requirements must be attacked") {
        val (driver, p) = driverWith(3)
        val theirs = driver.readyBear(p[1])
        // Goaded by P1 and P3; Kardur's requirement also names P3. Attacking P3 breaks two, P1 one.
        driver.replaceState(driver.state.updateEntity(theirs) { it.with(GoadedComponent(setOf(p[0], p[2]))) })
        driver.applyKardur(p[2])
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(p[1], mapOf(theirs to p[2])).error!! shouldContain "goaded"
        driver.declareAttackers(p[1], mapOf(theirs to p[0])).error shouldBe null
    }

    test("each creature it binds shows a Must Attack badge naming the player to avoid") {
        val (driver, p) = driverWith(3)
        val mine = driver.readyBear(p[0])
        val theirs = driver.readyBear(p[1])
        driver.applyKardur(p[0])

        val view = ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = driver.cardRegistry))
            .transform(driver.state, p[1])
        val badge = view.cards.getValue(theirs).activeEffects.single { it.effectId.startsWith("must_attack_group") }
        badge.description!! shouldContain "a player other than Player 1"
        view.cards.getValue(mine).activeEffects.none { it.effectId.startsWith("must_attack_group") } shouldBe true
    }

    test("the printed static names its holder's controller") {
        val (driver, p) = driverWith(3)
        driver.putPermanentOnBattlefield(p[0], "Test Kardur Banner")
        val theirs = driver.readyBear(p[1])
        driver.state.projectedState.mustAttack(theirs) shouldBe true
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(p[1], emptyMap()).error shouldNotBe null
        driver.declareAttackers(p[1], mapOf(theirs to p[0])).error!! shouldContain "a player other than"
        driver.declareAttackers(p[1], mapOf(theirs to p[2])).error shouldBe null
    }

    test("description reads as the card does") {
        kardurEffect.description shouldStartWith "Until your next turn, "
        kardurEffect.description shouldEndWith " attack each combat if able and attack a player other than you if able"
        MustAttack(GroupFilter.AllCreatures, MustAttackDefender.APlayer).description shouldBe
            "${GroupFilter.AllCreatures.description} attack a player each combat if able"
    }
})
