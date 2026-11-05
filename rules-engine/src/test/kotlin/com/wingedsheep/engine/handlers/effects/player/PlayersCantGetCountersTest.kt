package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.counters.ProliferateExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.PlayersCantGetCounters
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [PlayersCantGetCounters] — the static "Players can't get counters" (Solemnity; CR 122.1: players
 * can have counters too). Pins:
 * - no counter of any kind goes on a covered player — effect placement of poison and energy,
 *   proliferate, and combat damage from infect and toxic (infect damage then does nothing to the
 *   player: no poison, no life loss) — while counters they already have stay;
 * - `affected` is relative to the source's controller (`You`, `EachOpponent`, `Each`);
 * - a kind-scoped prohibition ("You can't get poison counters") refuses only that kind;
 * - a cost that would give the covered payer counters (ward "get five poison counters") can't be
 *   paid, so the spell is countered without a prompt;
 * - the prohibition is a static: it ends the moment the source leaves the battlefield, and a source
 *   that has lost all its abilities forbids nothing.
 */
class PlayersCantGetCountersTest : FunSpec({

    fun instant(name: String, script: CardScript) =
        CardDefinition.instant(name = name, manaCost = ManaCost.parse("{W}"), oracleText = name, script = script)

    fun warden(name: String, ability: PlayersCantGetCounters) = card(name) {
        manaCost = "{W}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        staticAbility { this.ability = ability }
    }

    val everyone = warden("Everyone Warden", PlayersCantGetCounters())
    val selfOnly = warden("Self Warden", PlayersCantGetCounters(affected = Player.You))
    val opponentsOnly = warden("Opponents Warden", PlayersCantGetCounters(affected = Player.EachOpponent))
    val poisonOnly = warden("Poison Warden", PlayersCantGetCounters(affected = Player.You, counterType = CounterType.POISON))

    val silence = card("Creature Silence") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        staticAbility { ability = LoseAllAbilities(filter = GroupFilter(GameObjectFilter.Creature)) }
    }

    val poisonMe = instant("Poison Me", CardScript.spell(Effects.AddCounters(CounterType.POISON, 2, EffectTarget.Controller)))
    val energyMe = instant("Energy Me", CardScript.spell(Effects.AddCounters(CounterType.ENERGY, 3, EffectTarget.Controller)))

    val poisonWardedBear = card("Poison-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.Ward(WardCost.PlayerCounters(CounterType.POISON, 5)))
    }

    val infectBear = card("Infect Bear") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.INFECT)
    }

    val toxicBear = card("Toxic Bear") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.toxic(2))
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                everyone, selfOnly, opponentsOnly, poisonOnly, silence, poisonMe, energyMe,
                poisonWardedBear, infectBear, toxicBear,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, id: EntityId, type: CounterType): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun cast(driver: GameTestDriver, player: EntityId, name: String) =
        driver.putCardInHand(player, name).let { card ->
            driver.giveMana(player, Color.WHITE, 1)
            driver.castSpell(player, card).also { if (it.error == null) driver.bothPass() }
        }

    test("no player gets poison or energy counters; counters they already have stay") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.addComponent(me, CountersComponent().withAdded(CounterType.ENERGY, 1))
        driver.putPermanentOnBattlefield(opponent, "Everyone Warden")

        driver.state.projectedState.canReceiveCounters(me) shouldBe false
        driver.state.projectedState.canReceiveCounters(opponent) shouldBe false
        cast(driver, me, "Poison Me").error shouldBe null
        cast(driver, me, "Energy Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 0
        withClue("the energy counter the player already had is untouched") {
            counters(driver, me, CounterType.ENERGY) shouldBe 1
        }
    }

    test("affected is read relative to the source's controller") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val self = driver.putPermanentOnBattlefield(me, "Self Warden")
        driver.state.cantGetCounters(me) shouldBe true
        driver.state.cantGetCounters(opponent) shouldBe false
        driver.moveToGraveyard(self)

        driver.putPermanentOnBattlefield(me, "Opponents Warden")
        driver.state.cantGetCounters(me) shouldBe false
        driver.state.cantGetCounters(opponent) shouldBe true
    }

    test("a kind-scoped prohibition refuses only that kind") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Poison Warden")

        withClue("the untyped question is about every kind, which a poison-only prohibition doesn't cover") {
            driver.state.projectedState.canReceiveCounters(me) shouldBe true
        }
        driver.state.projectedState.canReceiveCounters(me, CounterType.POISON) shouldBe false
        cast(driver, me, "Poison Me").error shouldBe null
        cast(driver, me, "Energy Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 0
        counters(driver, me, CounterType.ENERGY) shouldBe 3
    }

    test("proliferate skips a covered player") {
        val driver = newDriver()
        val me = driver.player1
        driver.addComponent(me, CountersComponent().withAdded(CounterType.POISON, 1))
        driver.putPermanentOnBattlefield(me, "Everyone Warden")

        val (after, _) = ProliferateExecutor.addOneOfEachKind(
            driver.state, listOf(me), driver.player2, PredicateEvaluator(cardRegistry = null)
        )
        after.getEntity(me)?.get<CountersComponent>()?.getCount(CounterType.POISON) shouldBe 1
    }

    test("infect and toxic combat damage gives a covered defender no poison") {
        val driver = newDriver()
        val me = driver.player1
        val defender = driver.player2
        driver.putPermanentOnBattlefield(defender, "Poison Warden")
        val infect = driver.putCreatureOnBattlefield(me, "Infect Bear")
        val toxic = driver.putCreatureOnBattlefield(me, "Toxic Bear")
        driver.removeSummoningSickness(infect)
        driver.removeSummoningSickness(toxic)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(infect, toxic), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()
        driver.bothPass()

        counters(driver, defender, CounterType.POISON) shouldBe 0
        withClue("infect damage to a player is only poison, so no life is lost; the toxic bear's damage is ordinary") {
            driver.getLifeTotal(defender) shouldBe 20 - 3
        }
    }

    test("a ward cost that gives the covered payer counters can't be paid — the spell is countered") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(opponent, "Everyone Warden")
        val bear = driver.putCreatureOnBattlefield(opponent, "Poison-Warded Bear")

        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        withClue("no pay-or-counter prompt is offered") {
            (driver.pendingDecision is YesNoDecision) shouldBe false
        }
        repeat(2) { if (driver.state.priorityPlayerId != null && driver.state.stack.isNotEmpty()) driver.bothPass() }
        counters(driver, me, CounterType.POISON) shouldBe 0
        driver.findPermanent(opponent, "Poison-Warded Bear") shouldBe bear
    }

    test("the prohibition ends when the source leaves the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        val warden = driver.putPermanentOnBattlefield(me, "Everyone Warden")
        driver.state.cantGetCounters(me) shouldBe true

        driver.moveToGraveyard(warden)
        driver.state.cantGetCounters(me) shouldBe false
        cast(driver, me, "Poison Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 2
    }

    test("a source that has lost all its abilities forbids nothing") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Everyone Warden")
        driver.putPermanentOnBattlefield(me, "Creature Silence")

        driver.state.cantGetCounters(me) shouldBe false
        cast(driver, me, "Poison Me").error shouldBe null
        counters(driver, me, CounterType.POISON) shouldBe 2
    }

    test("description reads the printed wording") {
        PlayersCantGetCounters().description shouldBe "Players can't get counters"
        PlayersCantGetCounters(Player.You, CounterType.POISON).description shouldBe "You can't get poison counters"
        PlayersCantGetCounters(Player.EachOpponent).description shouldBe "Your opponents can't get counters"
    }
})
