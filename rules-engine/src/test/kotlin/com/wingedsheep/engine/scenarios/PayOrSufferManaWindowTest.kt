package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.AshnodsAltar
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Sacrifice this unless you pay {2}" — [PayOrSufferEffect] with a mana cost.
 *
 * Agreeing to pay used to hand the cost straight to the auto-tap solver: the player couldn't choose
 * which permanents were tapped, and couldn't activate a mana ability to cover it (CR 605.3a). Once
 * `canPay` learned to count mana the solver won't auto-tap (a Treasure, an Ashnod's Altar), that
 * became an outright bug — the yes/no was offered, the player said yes, and the permanent was
 * sacrificed anyway because `solve()` came up empty.
 *
 * The fix gives this path the same second step ward and "counter unless you pay" have always had.
 */
class PayOrSufferManaWindowTest : FunSpec({

    /** "At the beginning of your upkeep, sacrifice this creature unless you pay {2}." */
    val upkeepToll = card("Upkeep Toll") {
        manaCost = "{1}"
        typeLine = "Creature — Spirit"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.YourUpkeep
            effect = PayOrSufferEffect(
                cost = Costs.pay.Mana("{2}"),
                suffer = SacrificeSelfEffect
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AshnodsAltar, upkeepToll))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    /** Ends the current turn so the controller's next upkeep fires the toll. */
    fun GameTestDriver.advanceToMyNextUpkeep() {
        passPriorityUntil(Step.END)
        bothPass()
        passPriorityUntil(Step.END)
        bothPass()
        passPriorityUntil(Step.UPKEEP)
    }

    fun GameTestDriver.manaPool(playerId: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(playerId)!!.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("agreeing to pay opens a mana source window instead of auto-tapping") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Upkeep Toll")
        val forestA = driver.putPermanentOnBattlefield(player, "Forest")
        driver.putPermanentOnBattlefield(player, "Forest")

        driver.advanceToMyNextUpkeep()
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(player, true)

        // The second step: which sources to tap.
        val window = driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        window.requiredCost shouldBe "{2}"
        window.canDecline shouldBe true

        // Pick one Forest by hand; auto-pay would have chosen for us.
        driver.submitDecision(
            player,
            ManaSourcesSelectedResponse(window.id, selectedSources = listOf(forestA))
        )
        // Only one Forest covers {1} of {2}, so the payment falls short and the toll is collected.
        driver.findPermanent(player, "Upkeep Toll") shouldBe null
    }

    test("the toll is paid with a mana ability activated inside the window") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val toll = driver.putCreatureOnBattlefield(player, "Upkeep Toll")
        val altar = driver.putPermanentOnBattlefield(player, "Ashnod's Altar")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        driver.advanceToMyNextUpkeep()
        driver.bothPass()

        // No lands at all — the only mana is "Sacrifice a creature: Add {C}{C}", which the solver
        // cannot auto-tap. The prompt is offered because canPay counts it.
        driver.submitYesNo(player, true)
        val window = driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        window.availableSources shouldBe emptyList()
        window.autoPaySuggestion shouldBe emptyList()

        // CR 605.3a — activate the Altar while the window is open. Its cost needs a creature
        // chosen, so the ability raises a decision of its own *inside* the window.
        val altarAbility = driver.cardRegistry.getCard("Ashnod's Altar")!!
            .script.activatedAbilities.first { it.isManaAbility }
        driver.submit(ActivateAbility(player, altar, altarAbility.id)).error shouldBe null

        val bears = driver.findPermanent(player, "Grizzly Bears").shouldNotBeNull()
        driver.submitCardSelection(player, listOf(bears))

        // The nested decision resolved and the window came back up on its own.
        driver.manaPool(player).colorless shouldBe 2
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        driver.submitDecision(player, ManaSourcesSelectedResponse(driver.pendingDecision!!.id))

        driver.state.getEntity(toll).shouldNotBeNull()
        driver.findPermanent(player, "Upkeep Toll").shouldNotBeNull()
        driver.manaPool(player).colorless shouldBe 0
    }

    test("declining in the window collects the toll, same as answering no") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Upkeep Toll")
        driver.putPermanentOnBattlefield(player, "Forest")
        driver.putPermanentOnBattlefield(player, "Forest")

        driver.advanceToMyNextUpkeep()
        driver.bothPass()
        driver.submitYesNo(player, true)

        val window = driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitDecision(player, ManaSourcesSelectedResponse(window.id, declined = true))

        driver.findPermanent(player, "Upkeep Toll") shouldBe null
    }

    test("floating mana pays without opening a window at all") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Upkeep Toll")

        driver.advanceToMyNextUpkeep()
        driver.giveMana(player, com.wingedsheep.sdk.core.Color.GREEN, 2)
        driver.bothPass()

        driver.submitYesNo(player, true)

        // Nothing to choose — the pool already covers it.
        driver.pendingDecision shouldBe null
        driver.findPermanent(player, "Upkeep Toll").shouldNotBeNull()
        driver.manaPool(player).green shouldBe 0
    }
})
