package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `PlayerHasMost` — "has the most X or is tied for the most" ([includeTies] = true) and "has more X
 * than each other player" ([includeTies] = false). The amount is measured once per player still in
 * the game with `Player.You` rebound to that player.
 */
class PlayerHasMostConditionTest : FunSpec({

    val hand = DynamicAmount.Count(Player.You, Zone.HAND)
    val life = DynamicAmount.LifeTotal(Player.You)
    val lands = DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land)

    // "Has flying as long as you have more cards in hand than each other player" — exercises the
    // projection-mode evaluation path.
    val handProud = card("Test Hand-Proud Bird") {
        manaCost = "{1}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
        oracleText = ""
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
                condition = Conditions.PlayerHasMoreThanEachOtherPlayer(Player.You, hand),
            )
        }
    }

    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + handProud)
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }

    fun GameTestDriver.setHandSize(player: EntityId, n: Int) {
        while (getHandSize(player) > n) moveToGraveyard(getHand(player).first())
        while (getHandSize(player) < n) putCardInHand(player, "Plains")
    }

    fun GameTestDriver.holds(condition: Condition, controller: EntityId): Boolean =
        services.conditionEvaluator.evaluate(state, condition, EffectContext(sourceId = null, controllerId = controller))

    test("strictly more than each other player: a tie for the top fails, a lone leader passes") {
        val (d, p) = threePlayer()
        d.setHandSize(p[0], 3)
        d.setHandSize(p[1], 5)
        d.setHandSize(p[2], 5)
        val strict = Conditions.PlayerHasMoreThanEachOtherPlayer(Player.You, hand) as Condition
        val orTied = Conditions.PlayerHasMost(Player.You, hand) as Condition

        withClue("tied for the most") {
            d.holds(strict, p[1]) shouldBe false
            d.holds(orTied, p[1]) shouldBe true
        }
        withClue("fewest") {
            d.holds(strict, p[0]) shouldBe false
            d.holds(orTied, p[0]) shouldBe false
        }

        d.setHandSize(p[2], 4)
        d.holds(strict, p[1]) shouldBe true
        d.holds(orTied, p[1]) shouldBe true
    }

    test("the named player is resolved through its reference; the controller is just another player measured") {
        val (d, p) = threePlayer()
        d.activePlayer shouldBe p[0]
        val activeHasMostLife = Conditions.PlayerHasMoreThanEachOtherPlayer(Player.ActivePlayer, life) as Condition
        d.setLifeTotal(p[0], 25)
        d.setLifeTotal(p[1], 30)
        d.setLifeTotal(p[2], 10)
        withClue("asked from p[1]'s side, the active player p[0] is behind p[1] itself") {
            d.holds(activeHasMostLife, p[1]) shouldBe false
        }
        d.setLifeTotal(p[1], 20)
        d.holds(activeHasMostLife, p[1]) shouldBe true
        d.holds(activeHasMostLife, p[2]) shouldBe true
    }

    test("battlefield counts are per controller") {
        val (d, p) = threePlayer()
        repeat(2) { d.putLandOnBattlefield(p[1], "Plains") }
        d.putLandOnBattlefield(p[2], "Plains")
        val strict = Conditions.PlayerHasMoreThanEachOtherPlayer(Player.You, lands) as Condition
        d.holds(strict, p[1]) shouldBe true
        d.holds(strict, p[2]) shouldBe false
        d.putLandOnBattlefield(p[2], "Plains")
        d.holds(strict, p[1]) shouldBe false
        d.holds(Conditions.PlayerControlsMostPermanents(Player.You, GameObjectFilter.Land) as Condition, p[1]) shouldBe true
    }

    test("a player who has left the game is not compared against") {
        val (d, p) = threePlayer()
        d.setHandSize(p[0], 4)
        d.setHandSize(p[1], 2)
        d.setHandSize(p[2], 6)
        val strict = Conditions.PlayerHasMoreThanEachOtherPlayer(Player.You, hand) as Condition
        d.holds(strict, p[0]) shouldBe false
        d.replaceState(d.state.updateEntity(p[2]) { it.with(PlayerLostComponent(LossReason.LIFE_ZERO)) })
        d.holds(strict, p[0]) shouldBe true
    }

    test("as a static ability's condition it is re-read as hands change (projection path)") {
        val (d, p) = threePlayer()
        val bird = d.putCreatureOnBattlefield(p[0], "Test Hand-Proud Bird")
        d.setHandSize(p[0], 6)
        d.setHandSize(p[1], 5)
        d.setHandSize(p[2], 5)
        d.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true

        d.setHandSize(p[1], 6)
        withClue("tied with an opponent: no longer more than each other player") {
            d.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe false
        }
    }

    test("descriptions read as the printed wording") {
        Conditions.PlayerHasMoreThanEachOtherPlayer(Player.TriggeringPlayer, hand).description shouldBe
            "if ${Player.TriggeringPlayer.description} has more cards in hand than each other player"
        Conditions.PlayerHasMoreThanEachOtherPlayer(Player.TriggeringPlayer, lands).description shouldBe
            "if ${Player.TriggeringPlayer.description} controls more lands than each other player"
        Conditions.PlayerHasMostLife(Player.You).description shouldBe
            "if ${Player.You.description} has the most life or is tied for the most"
    }
})
