package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.costs.CostAtomAmounts
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.PlayersCantPayLifeForCosts
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Life costs priced from game state — [CostAtom.PayLife] carrying a
 * [DynamicAmount.CommanderColorIdentityCount]
 * (War Room's "Pay life equal to the number of colors in your commanders' color identity").
 *
 * Pins:
 *  - [DynamicAmount.CommanderColorIdentityCount] counts the union of a
 *    Partner pair's identities, alike through the effect evaluator and the cost pricer;
 *  - the activation charges that many life, read for the activating player;
 *  - a player whose life is below the price is neither offered the activation nor allowed to
 *    submit it (CR 119.4) — the enumerator and the validator agree;
 *  - a colorless commander prices it at 0, payable even where no life may be paid (CR 119.4b);
 *  - a player with no commander can't activate it at all (War Room's ruling);
 *  - the atom's whitelist and printed text.
 */
class DynamicLifeCostTest : FunSpec({

    val lifeEngine = card("Commander Life Engine") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}, Pay life equal to the number of colors in your commanders' color identity: Draw a card."
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.PayLife(DynamicAmounts.commanderColorIdentityCount()))
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }

    fun legend(name: String, cost: String, partner: Boolean = false): CardDefinition {
        val def = CardDefinition.creature(
            name = name,
            manaCost = ManaCost.parse(cost),
            subtypes = setOf(Subtype("Human")),
            power = 2,
            toughness = 2,
            supertypes = setOf(Supertype.LEGENDARY),
            oracleText = if (partner) "Partner" else "",
        )
        return if (partner) def.copy(keywordAbilities = listOf(KeywordAbility.partner())) else def
    }

    val whitePartner = legend("Dynamic Life White Partner", "{1}{W}", partner = true)
    val bluePartner = legend("Dynamic Life Blue Partner", "{1}{U}", partner = true)
    val greenCommander = legend("Dynamic Life Green Commander", "{1}{G}")
    val colorlessCommander = legend("Dynamic Life Colorless Commander", "{3}")
    val filler = CardDefinition.creature(
        name = "Dynamic Life Filler", manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")), power = 1, toughness = 1,
    )
    val lifeLock = card("Dynamic Life Cost Lock") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "Players can't pay life to cast spells or to activate abilities."
        staticAbility { ability = PlayersCantPayLifeForCosts() }
    }
    val all = listOf(lifeLock, lifeEngine, whitePartner, bluePartner, greenCommander, colorlessCommander, filler)

    /** Seat 0 commands the white/blue partners; seat 1 [secondCommander]. */
    fun commanderGame(secondCommander: CardDefinition = greenCommander): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(all)
        val players = driver.initMultiplayer(
            decks = listOf(Deck(cards = List(98) { filler.name }), Deck(cards = List(99) { filler.name })),
            format = Format.Commander(),
            commanders = listOf(whitePartner.name, secondCommander.name),
            partners = listOf(bluePartner.name, null),
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    val abilityId = lifeEngine.activatedAbilities.single().id
    val lifeAtom = CostAtom.PayLife(DynamicAmounts.commanderColorIdentityCount())

    fun GameTestDriver.canActivate(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.affordable }

    fun GameTestDriver.price(player: EntityId) =
        CostAtomAmounts.life(state, lifeAtom, player, services.predicateEvaluator)

    test("CommanderColorIdentityCount counts a Partner pair's union, as effect and as cost") {
        val (driver, players) = commanderGame()
        val (partnered, single) = players
        val amount = DynamicAmount.CommanderColorIdentityCount()
        fun asEffect(player: EntityId) =
            driver.services.predicateEvaluator.amounts.evaluate(driver.state, amount, EffectContext(sourceId = null, controllerId = player))

        asEffect(partnered) shouldBe 2
        asEffect(single) shouldBe 1
        driver.price(partnered) shouldBe 2
        driver.price(single) shouldBe 1
        withClue("no payer to read the identity off: unpayable, never free") {
            CostAtomAmounts.evaluate(driver.state, amount) shouldBe CostAtomAmounts.UNPRICEABLE
        }
    }

    test("activating charges the activating player's commander-identity count") {
        val (driver, players) = commanderGame()
        val (partnered, single) = players
        driver.setLifeTotal(partnered, 10)
        driver.setLifeTotal(single, 10)
        val mine = driver.putPermanentOnBattlefield(partnered, lifeEngine.name)
        val handBefore = driver.getHandSize(partnered)

        driver.canActivate(partnered, mine) shouldBe true
        driver.submitSuccess(ActivateAbility(playerId = partnered, sourceId = mine, abilityId = abilityId))
        driver.getLifeTotal(partnered) shouldBe 8
        driver.bothPass()
        driver.getHandSize(partnered) shouldBe handBefore + 1
        withClue("the opponent's single-color identity is not what the activator pays") {
            driver.getLifeTotal(single) shouldBe 10
        }
    }

    test("life below the price: not offered, and rejected when submitted (CR 119.4)") {
        val (driver, players) = commanderGame()
        val partnered = players[0]
        val engine = driver.putPermanentOnBattlefield(partnered, lifeEngine.name)
        driver.setLifeTotal(partnered, 1)

        driver.canActivate(partnered, engine) shouldBe false
        driver.submit(ActivateAbility(playerId = partnered, sourceId = engine, abilityId = abilityId)).error shouldNotBe null
        driver.getLifeTotal(partnered) shouldBe 1

        withClue("exactly enough life is enough — paying down to 0 is legal") {
            driver.setLifeTotal(partnered, 2)
            driver.canActivate(partnered, engine) shouldBe true
            driver.submitSuccess(ActivateAbility(playerId = partnered, sourceId = engine, abilityId = abilityId))
            driver.getLifeTotal(partnered) shouldBe 0
        }
    }

    test("a colorless commander prices it at 0 life") {
        val (driver, players) = commanderGame(secondCommander = colorlessCommander)
        val colorless = players[1]
        driver.price(colorless) shouldBe 0
        driver.setLifeTotal(colorless, 5)
        val engine = driver.putPermanentOnBattlefield(colorless, lifeEngine.name)
        driver.passPriority(players[0])

        driver.canActivate(colorless, engine) shouldBe true
        driver.submitSuccess(ActivateAbility(playerId = colorless, sourceId = engine, abilityId = abilityId))
        driver.getLifeTotal(colorless) shouldBe 5
    }

    test("paying 0 life is always allowed, even under a can't-pay-life lock (CR 119.4b)") {
        val (driver, players) = commanderGame(secondCommander = colorlessCommander)
        val (partnered, colorless) = players
        driver.putPermanentOnBattlefield(partnered, lifeLock.name)
        val theirs = driver.putPermanentOnBattlefield(partnered, lifeEngine.name)
        val engine = driver.putPermanentOnBattlefield(colorless, lifeEngine.name)

        withClue("the two-color player owes 2 life, which the lock forbids") {
            driver.canActivate(partnered, theirs) shouldBe false
            driver.submit(ActivateAbility(playerId = partnered, sourceId = theirs, abilityId = abilityId)).error shouldNotBe null
        }
        driver.passPriority(partnered)
        withClue("the colorless-commander player owes 0, which no lock can forbid") {
            driver.canActivate(colorless, engine) shouldBe true
            driver.submitSuccess(ActivateAbility(playerId = colorless, sourceId = engine, abilityId = abilityId))
        }
    }

    test("a player with no commander can't activate it at all (War Room ruling)") {
        val driver = GameTestDriver()
        driver.registerCards(all)
        driver.initMirrorMatch(deck = Deck.of(filler.name to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val engine = driver.putPermanentOnBattlefield(me, lifeEngine.name)

        driver.price(me) shouldBe CostAtomAmounts.UNPRICEABLE
        driver.canActivate(me, engine) shouldBe false
        driver.submit(ActivateAbility(playerId = me, sourceId = engine, abilityId = abilityId)).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 20
    }

    test("the atom admits only nonnegative constants and state-priced counts, and prints both") {
        CostAtom.PayLife(2).description shouldBe "pay 2 life"
        lifeAtom.description shouldBe "pay life equal to the number of colors in your commanders' color identity"
        CostAtom.PayLife(0).fixedAmount shouldBe 0
        lifeAtom.fixedAmount shouldBe null
        shouldThrow<IllegalArgumentException> { CostAtom.PayLife(-1) }
        shouldThrow<IllegalArgumentException> { CostAtom.PayLife(DynamicAmount.XValue) }
        withClue("a cost is priced for its payer alone") {
            shouldThrow<IllegalArgumentException> {
                CostAtom.PayLife(DynamicAmount.CommanderColorIdentityCount(Player.TargetOpponent))
            }
        }
        DynamicAmount.CommanderColorIdentityCount(Player.TargetOpponent).description shouldBe
            "the number of colors in target opponent's commanders' color identity"
    }
})
