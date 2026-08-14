package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.RadiantLotus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Radiant Lotus — "{T}, Sacrifice one or more artifacts: Choose a color. Target player adds three
 * mana of the chosen color for each artifact sacrificed this way."
 *
 * Exercises both halves of the feature end to end:
 *
 *  - `Costs.SacrificePermanents` — the variable-count *sacrifice* cost whose X is the **count** of
 *    permanents chosen (CR 601.2b), not their total mana value. The chosen artifacts must land in
 *    the graveyard (a sacrifice, CR 701.17a), not in exile.
 *  - `AddManaOfChoiceEffect.recipient` — the mana goes to the **target player's** pool while the
 *    colour is still the controller's choice.
 *
 * Also pins the timing consequences: because the ability targets it is *not* a mana ability
 * (CR 605.1a), so it uses the stack and no mana appears until it resolves.
 */
class RadiantLotusScenarioTest : FunSpec({

    // A plain artifact to feed the sacrifice cost, and a non-artifact permanent that must not be
    // an eligible choice for it.
    val trinket = card("Test Trinket") { manaCost = "{1}"; typeLine = "Artifact" }
    val totem = card("Test Totem") { manaCost = "{2}"; typeLine = "Enchantment" }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RadiantLotus, trinket, totem))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val abilityId = RadiantLotus.activatedAbilities.first().id

    fun GameTestDriver.pool(playerId: com.wingedsheep.sdk.model.EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("sacrificing one artifact gives the target player three mana of the chosen colour") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val sacrificed = d.putPermanentOnBattlefield(you, "Test Trinket")

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(sacrificed))
            )
        )
        d.bothPass()

        // "Choose a color" — the controller picks, not the recipient.
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.BLUE))

        // Three mana of the chosen colour, in the *target's* pool only.
        d.pool(opponent).getAmount(Color.BLUE) shouldBe 3
        d.pool(opponent).getAmount(Color.RED) shouldBe 0
        d.pool(you).getAmount(Color.BLUE) shouldBe 0
    }

    test("three mana per artifact — sacrificing three artifacts adds nine") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val a = d.putPermanentOnBattlefield(you, "Test Trinket")
        val b = d.putPermanentOnBattlefield(you, "Test Trinket")
        val c = d.putPermanentOnBattlefield(you, "Test Trinket")

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(a, b, c))
            )
        )
        d.bothPass()
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.GREEN))

        // X is the *count* of artifacts sacrificed (3), not their total mana value (also 3 here by
        // coincidence of MV1 each — the next test separates the two measures).
        d.pool(opponent).getAmount(Color.GREEN) shouldBe 9
    }

    test("X is the count of artifacts sacrificed, not their total mana value") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        // Two artifacts of mana value 1 and 6: total MV 7, count 2.
        val cheap = d.putPermanentOnBattlefield(you, "Test Trinket")
        val expensive = d.putPermanentOnBattlefield(you, "Radiant Lotus")

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(cheap, expensive))
            )
        )
        d.bothPass()
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.WHITE))

        // 2 artifacts × 3 = 6. A TOTAL_MANA_VALUE measure would have produced 21.
        d.pool(opponent).getAmount(Color.WHITE) shouldBe 6
    }

    test("the cost is a sacrifice — the artifacts go to the graveyard, not exile") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val sacrificed = d.putPermanentOnBattlefield(you, "Test Trinket")

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(sacrificed))
            )
        )

        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(sacrificed) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.EXILE)).contains(sacrificed) shouldBe false
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(sacrificed) shouldBe false
    }

    test("Radiant Lotus may sacrifice itself to its own cost") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")

        // The only artifact on the battlefield is the Lotus itself — it is a legal choice, so the
        // ability is activatable with nothing else in play (`excludeSelf = false`).
        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(lotus))
            )
        )
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(lotus) shouldBe true

        d.bothPass()
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.BLACK))

        // The ability resolves even though its source is gone — it is independent of the source
        // once on the stack (CR 113.7a).
        d.pool(opponent).getAmount(Color.BLACK) shouldBe 3
    }

    test("the target player may be yourself") {
        val d = setup()
        val you = d.activePlayer!!

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val sacrificed = d.putPermanentOnBattlefield(you, "Test Trinket")

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(you)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(sacrificed))
            )
        )
        d.bothPass()
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.RED))

        d.pool(you).getAmount(Color.RED) shouldBe 3
    }

    test("it is not a mana ability — no mana appears until the ability resolves") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val sacrificed = d.putPermanentOnBattlefield(you, "Test Trinket")

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(sacrificed))
            )
        )

        // Because it targets, it uses the stack (CR 605.1a): the cost is paid but no mana yet.
        d.state.stack.size shouldBe 1
        d.pool(opponent).getAmount(Color.BLUE) shouldBe 0
        d.pool(opponent).getAmount(Color.RED) shouldBe 0
    }

    test("a non-artifact permanent cannot pay the sacrifice cost") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val enchantment = d.putPermanentOnBattlefield(you, "Test Totem")

        val result = d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = lotus,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(enchantment))
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull()
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(enchantment) shouldBe true
    }

    test("UI flow: a bare activation pauses for the sacrifice choice, then the target, then the colour") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val a = d.putPermanentOnBattlefield(you, "Test Trinket")
        val b = d.putPermanentOnBattlefield(you, "Test Trinket")
        val totemId = d.putPermanentOnBattlefield(you, "Test Totem")

        // The client submits the bare action — no cost selection, no target. The engine pauses
        // rather than succeeding outright.
        d.submit(ActivateAbility(playerId = you, sourceId = lotus, abilityId = abilityId))
            .isPaused shouldBe true

        // 1. Which artifacts to sacrifice. All three artifacts (including the Lotus) are offered;
        //    the enchantment is not.
        val sacrificeDecision = d.pendingDecision
        sacrificeDecision.shouldBeInstanceOf<SelectCardsDecision>()
        sacrificeDecision.options.toSet() shouldBe setOf(lotus, a, b)
        sacrificeDecision.options.contains(totemId) shouldBe false
        sacrificeDecision.minSelections shouldBe 1
        sacrificeDecision.maxSelections shouldBe 3
        d.submitDecision(you, CardsSelectedResponse(sacrificeDecision.id, listOf(a, b)))

        // 2. Which player to target.
        val targetDecision = d.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitDecision(you, TargetsResponse(targetDecision.id, mapOf(0 to listOf(opponent))))

        // The cost is paid and the ability is on the stack.
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(a) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(b) shouldBe true
        d.bothPass()

        // 3. Which colour, chosen by the controller as the ability resolves.
        val colorDecision = d.pendingDecision
        colorDecision.shouldBeInstanceOf<ChooseColorDecision>()
        d.submitDecision(you, ColorChosenResponse(colorDecision.id, Color.RED))

        d.pool(opponent).getAmount(Color.RED) shouldBe 6
    }

    test("cancelling the sacrifice choice leaves the board untouched") {
        val d = setup()
        val you = d.activePlayer!!

        val lotus = d.putPermanentOnBattlefield(you, "Radiant Lotus")
        val sacrificed = d.putPermanentOnBattlefield(you, "Test Trinket")

        d.submit(ActivateAbility(playerId = you, sourceId = lotus, abilityId = abilityId))
            .isPaused shouldBe true
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, com.wingedsheep.engine.core.CancelDecisionResponse(decision.id))

        // The cost is paid after the pause, so backing out costs nothing.
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(sacrificed) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(lotus) shouldBe true
        d.isTapped(lotus) shouldBe false
        d.state.stack.size shouldBe 0
    }
})
