package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Which graveyards a [MayCastFromGraveyard] permission reaches, and who may hold one.
 *
 *  - By default only the holder's own graveyard; `fromAnyGraveyard = true` reaches every player's
 *    (The Great Work's "cast instant and sorcery spells from any graveyard").
 *  - A durational grant anchored to the *player* is a player-wide permission that needs no
 *    permanent to hang off — so it outlives the Saga that created it.
 *  - A card cast from another player's graveyard is still a graveyard cast (`castFromZone`), and
 *    the exile rider sends it to its *owner's* exile.
 *  - The legal-action enumerator offers exactly what the handler accepts.
 */
class GraveyardCastScopeTest : FunSpec({

    val sorcery = card("Scope Test Sorcery") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }
    val bear = card("Scope Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(sorcery, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun grantToPlayer(driver: GameTestDriver, player: EntityId, ability: MayCastFromGraveyard) {
        driver.replaceState(
            driver.state.copy(
                grantedStaticAbilities = driver.state.grantedStaticAbilities +
                    GrantedStaticAbility(entityId = player, ability = ability, duration = Duration.EndOfTurn)
            )
        )
    }

    fun offered(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean =
        driver.legalActions(player).any {
            (it.action as? com.wingedsheep.engine.core.CastSpell)?.cardId == cardId
        }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("a player-anchored grant without fromAnyGraveyard covers only your own graveyard") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        grantToPlayer(driver, you, MayCastFromGraveyard(GameObjectFilter.InstantOrSorcery))
        val mine = driver.putCardInGraveyard(you, "Scope Test Sorcery")
        val theirs = driver.putCardInGraveyard(opp, "Scope Test Sorcery")

        offered(driver, you, mine) shouldBe true
        offered(driver, you, theirs) shouldBe false
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, theirs).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(opp).contains(theirs) shouldBe true
    }

    test("fromAnyGraveyard reaches an opponent's graveyard; the exile rider uses the owner's exile") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        grantToPlayer(
            driver, you,
            MayCastFromGraveyard(GameObjectFilter.InstantOrSorcery, exileInsteadOfGraveyard = true, fromAnyGraveyard = true)
        )
        val theirs = driver.putCardInGraveyard(opp, "Scope Test Sorcery")

        offered(driver, you, theirs) shouldBe true
        withClue("the grant is yours — the opponent can't use it") {
            offered(driver, opp, theirs) shouldBe false
        }
        val lifeBefore = driver.getLifeTotal(you)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, theirs).outcome shouldBe Outcome.Done
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore + 3
        driver.getGraveyard(opp).contains(theirs) shouldBe false
        driver.getExile(opp).contains(theirs) shouldBe true
    }

    test("a permanent cast out of an opponent's graveyard is recorded as cast from a graveyard") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        grantToPlayer(driver, you, MayCastFromGraveyard(GameObjectFilter.Creature, fromAnyGraveyard = true))
        val theirBear = driver.putCardInGraveyard(opp, "Scope Test Bear")

        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, theirBear).outcome shouldBe Outcome.Done
        resolveStack(driver)

        driver.state.getBattlefield(you).contains(theirBear) shouldBe true
        driver.state.getEntity(theirBear)!!.has<CastFromGraveyardComponent>() shouldBe true
    }

    test("a player-anchored grant expires in the cleanup step") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        grantToPlayer(driver, you, MayCastFromGraveyard(GameObjectFilter.InstantOrSorcery, fromAnyGraveyard = true))
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        driver.state.grantedStaticAbilities.none { it.entityId == you } shouldBe true
    }
})
