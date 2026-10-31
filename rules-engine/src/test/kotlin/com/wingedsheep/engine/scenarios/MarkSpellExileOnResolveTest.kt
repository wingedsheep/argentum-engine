package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ExiledSpellRider
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `MarkSpellExileOnResolveEffect(then = RETURN_TO_HAND_AT_NEXT_END_STEP)` — "exile that card instead
 * of putting it into your graveyard as it resolves. If you do, return it to your hand at the
 * beginning of the next end step." (Feather, the Redeemed). The rulings pinned here:
 *  - the spell resolves fully, lands in exile, and comes back to hand at the next end step;
 *  - a spell that doesn't resolve (countered) is put into the graveyard and not returned;
 *  - a spell resolved during the end step comes back at the *next* turn's end step;
 *  - a spell whose own instructions exile it was never headed for the graveyard, so it isn't
 *    returned.
 */
class MarkSpellExileOnResolveTest : FunSpec({

    val redeemer = card("Test Redeemer") {
        manaCost = "{R}{W}{W}"
        typeLine = "Creature — Angel"
        power = 3
        toughness = 4
        triggeredAbility {
            trigger = Triggers.you.casts(
                GameObjectFilter.InstantOrSorcery,
                requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl()))
            )
            effect = Effects.MarkSpellExileOnResolve(ExiledSpellRider.RETURN_TO_HAND_AT_NEXT_END_STEP)
        }
    }

    val fleetingBoost = card("Fleeting Boost") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 1, t)
            selfExile()
        }
    }

    fun createDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(redeemer, fleetingBoost))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        val angel = driver.putCreatureOnBattlefield(player, "Test Redeemer")
        return driver to angel
    }

    fun castOn(driver: GameTestDriver, player: EntityId, name: String, target: EntityId): EntityId {
        val spell = driver.putCardInHand(player, name)
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, spell, listOf(target)).error shouldBe null
        return spell
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 10) driver.bothPass()
    }

    test("the spell resolves, is exiled instead of going to the graveyard, and returns to hand at the next end step") {
        val (driver, angel) = createDriver()
        val player = driver.activePlayer!!
        val growth = castOn(driver, player, "Giant Growth", angel)
        resolveAll(driver)

        withClue("Giant Growth resolved") { driver.state.projectedState.getPower(angel) shouldBe 6 }
        withClue("exiled, not in the graveyard") {
            driver.getExile(player).contains(growth) shouldBe true
            driver.getGraveyard(player).contains(growth) shouldBe false
        }

        driver.passPriorityUntil(Step.END)
        resolveAll(driver)
        withClue("returned to its owner's hand at the beginning of the end step") {
            driver.getHand(player).contains(growth) shouldBe true
            driver.getExile(player).contains(growth) shouldBe false
        }
    }

    test("a countered spell goes to the graveyard and is not returned") {
        val (driver, angel) = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val growth = castOn(driver, player, "Giant Growth", angel)
        driver.bothPass() // resolve the cast trigger; Giant Growth stays on the stack

        driver.passPriority(player)
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(growth)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        resolveAll(driver)

        driver.getGraveyard(player).contains(growth) shouldBe true
        driver.passPriorityUntil(Step.END)
        resolveAll(driver)
        withClue("nothing comes back") { driver.getHand(player).contains(growth) shouldBe false }
        driver.getGraveyard(player).contains(growth) shouldBe true
    }

    test("a spell resolved during the end step returns at the next turn's end step") {
        val (driver, angel) = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.END)
        val growth = castOn(driver, player, "Giant Growth", angel)
        resolveAll(driver)
        driver.getExile(player).contains(growth) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        withClue("still exiled after this turn's end step") {
            driver.getExile(player).contains(growth) shouldBe true
        }
        driver.passPriorityUntil(Step.END)
        resolveAll(driver)
        withClue("returned at the next end step (the opponent's turn)") {
            driver.getHand(player).contains(growth) shouldBe true
        }
    }

    test("a spell that exiles itself was never headed for the graveyard, so it isn't returned") {
        val (driver, angel) = createDriver()
        val player = driver.activePlayer!!
        val boost = castOn(driver, player, "Fleeting Boost", angel)
        resolveAll(driver)
        driver.getExile(player).contains(boost) shouldBe true

        driver.passPriorityUntil(Step.END)
        resolveAll(driver)
        withClue("stays in exile") {
            driver.getExile(player).contains(boost) shouldBe true
            driver.getHand(player).contains(boost) shouldBe false
        }
    }
})
