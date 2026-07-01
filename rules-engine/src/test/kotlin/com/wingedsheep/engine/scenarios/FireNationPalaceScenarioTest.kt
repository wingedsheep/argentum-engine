package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.FireNationPalace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fire Nation Palace (TLA) — Land — Rare.
 *
 * - This land enters tapped unless you control a basic land.
 * - {T}: Add {R}.
 * - {1}{R}, {T}: Target creature you control gains firebending 4 until end of turn.
 *   (Whenever it attacks, add {R}{R}{R}{R}. This mana lasts until end of combat.)
 *
 * The grant is the genuinely new piece: firebending has no engine handler, so the grant must
 * install the *same* attack-trigger→combat-mana behavior the printed keyword installs. These
 * tests pin the enters-tapped condition, the {T}:{R} mana ability, and — the heart of the card —
 * that the granted firebending actually fires on attack and produces four combat-duration reds,
 * mirroring [FirebendingScenarioTest]'s attack→mana assertion.
 */
class FireNationPalaceScenarioTest : FunSpec({

    // Vanilla beater used as the firebending grant's target/attacker.
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FireNationPalace, bear))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Fully resolve the stack, resolving every triggered ability that lands on it. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.combatMana(playerId: EntityId) =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .filter { it.expiry == ManaExpiry.END_OF_COMBAT }

    test("enters tapped when you control no basic land") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val palace = driver.putCardInHand(me, "Fire Nation Palace")
        driver.playLand(me, palace).isSuccess shouldBe true

        driver.isTapped(palace) shouldBe true
    }

    test("enters untapped when you control a basic land") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(me, "Mountain") // a basic land we control
        val palace = driver.putCardInHand(me, "Fire Nation Palace")
        driver.playLand(me, palace).isSuccess shouldBe true

        driver.isTapped(palace) shouldBe false
    }

    test("{T}: Add {R} produces one red mana") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val palace = driver.putLandOnBattlefield(me, "Fire Nation Palace")
        val manaAbility = FireNationPalace.activatedAbilities[0].id

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = palace, abilityId = manaAbility))

        driver.state.getEntity(me)?.get<ManaPoolComponent>()?.red shouldBe 1
        driver.isTapped(palace) shouldBe true
    }

    test("{1}{R},{T}: target creature gains firebending 4; it adds {R}{R}{R}{R} on attack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(me, "Test Bear")
        driver.removeSummoningSickness(creature)
        val palace = driver.putLandOnBattlefield(me, "Fire Nation Palace")

        // Pay {1}{R} from pool, tap the palace, target our creature.
        driver.giveMana(me, Color.RED, 2)
        val grantAbility = FireNationPalace.activatedAbilities[1].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = palace,
                abilityId = grantAbility,
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        )
        driver.resolveStack() // resolve the grant ability

        // The creature now carries the granted firebending trigger.
        driver.state.grantedTriggeredAbilities.any { it.entityId == creature } shouldBe true

        // When it attacks this turn, firebending 4 adds four combat-duration reds.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(creature), opponent)
        driver.resolveStack()

        val combat = driver.combatMana(me)
        combat.size shouldBe 4
        combat.all { it.color == Color.RED } shouldBe true
    }

    test("the granted firebending is gone after end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(me, "Test Bear")
        val palace = driver.putLandOnBattlefield(me, "Fire Nation Palace")

        driver.giveMana(me, Color.RED, 2)
        val grantAbility = FireNationPalace.activatedAbilities[1].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = palace,
                abilityId = grantAbility,
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        )
        driver.resolveStack()
        driver.state.grantedTriggeredAbilities.any { it.entityId == creature } shouldBe true

        // Cross this turn's cleanup into the next turn (cleanup clears EndOfTurn grants).
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.grantedTriggeredAbilities.any { it.entityId == creature } shouldBe false
    }
})
