package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * A mana ability that reads "Activate only as a sorcery" (Grinning Ignus).
 *
 * CR 602.5d: it may be activated only when its controller could cast a sorcery — their main phase,
 * holding priority, stack empty. It is still a mana ability (CR 605.1a), so it adds its mana
 * immediately without using the stack. And because a cost is paid with the spell or ability already
 * on the stack (CR 601.2a, 601.2g), it can never be activated mid-payment: the auto-payer neither
 * spends it nor counts it toward affordability. The player activates it first and pays from the
 * floating mana.
 */
class SorcerySpeedManaAbilityTest : FunSpec({

    // {R}, Return this creature to its owner's hand: Add {C}{C}{R}. Activate only as a sorcery.
    val ignus = card("Sorcery Mana Ignus") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{R}"), Costs.ReturnSelfToHand)
            effect = Effects.AddColorlessMana(2) then Effects.AddMana(Color.RED, 1)
            manaAbility = true
            timing = TimingRule.SorcerySpeed
        }
    }

    // {3}: a spell the board can pay only through the ignus's mana.
    val golem = card("Sorcery Mana Golem") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 3
        toughness = 3
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ignus, golem))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val abilityId = ignus.activatedAbilities.single().id

    fun GameTestDriver.ignusOffered(playerId: EntityId, ignusId: EntityId): Boolean =
        legalActions(playerId).any { (it.action as? ActivateAbility)?.let { a -> a.sourceId == ignusId && a.abilityId == abilityId } == true }

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("the builder keeps sorcery timing on a mana ability; plain mana abilities still derive ManaAbility") {
        val ability = ignus.activatedAbilities.single()
        ability.isManaAbility shouldBe true
        ability.timing shouldBe TimingRule.SorcerySpeed

        val plain = card("Sorcery Mana Plain Druid") {
            manaCost = "{G}"
            typeLine = "Creature — Elf Druid"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
                manaAbility = true
            }
        }
        plain.activatedAbilities.single().timing shouldBe TimingRule.ManaAbility
    }

    test("offered in its controller's main phase with an empty stack, and resolves without using the stack") {
        val d = driver()
        val you = d.activePlayer!!
        d.putLandOnBattlefield(you, "Mountain")
        val ignusId = d.putPermanentOnBattlefield(you, "Sorcery Mana Ignus")

        d.ignusOffered(you, ignusId) shouldBe true
        d.submit(ActivateAbility(you, ignusId, abilityId)).error shouldBe null

        d.stackSize shouldBe 0
        d.pool(you).colorless shouldBe 2
        d.pool(you).red shouldBe 1
        d.state.getZone(com.wingedsheep.engine.state.ZoneKey(you, Zone.HAND)).contains(ignusId) shouldBe true
    }

    test("not offered and rejected outside sorcery timing — another step, or the opponent's turn") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putLandOnBattlefield(you, "Mountain")
        val ignusId = d.putPermanentOnBattlefield(you, "Sorcery Mana Ignus")

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.ignusOffered(you, ignusId) shouldBe false
        d.submit(ActivateAbility(you, ignusId, abilityId)).error.shouldNotBeNull() shouldContain "sorcery"

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opponent)
        d.ignusOffered(you, ignusId) shouldBe false
    }

    test("not offered while a spell is on the stack") {
        val d = driver()
        val you = d.activePlayer!!
        d.putLandOnBattlefield(you, "Mountain")
        val ignusId = d.putPermanentOnBattlefield(you, "Sorcery Mana Ignus")
        d.giveColorlessMana(you, 3)
        d.submit(CastSpell(you, d.putCardInHand(you, "Sorcery Mana Golem"),
            paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool)).error shouldBe null

        d.stackSize shouldBe 1
        d.ignusOffered(you, ignusId) shouldBe false
        d.submit(ActivateAbility(you, ignusId, abilityId)).error.shouldNotBeNull()
    }

    test("auto-pay never spends it: a {3} spell with one Mountain and the ignus is neither affordable nor castable") {
        val d = driver()
        val you = d.activePlayer!!
        d.putLandOnBattlefield(you, "Mountain")
        d.putPermanentOnBattlefield(you, "Sorcery Mana Ignus")
        val golemId = d.putCardInHand(you, "Sorcery Mana Golem")

        d.legalActions(you).none { (it.action as? CastSpell)?.cardId == golemId && it.affordable } shouldBe true
        d.submit(CastSpell(you, golemId)).error.shouldNotBeNull()
    }

    test("activated first at sorcery speed, its floating mana pays for the spell") {
        val d = driver()
        val you = d.activePlayer!!
        d.putLandOnBattlefield(you, "Mountain")
        val ignusId = d.putPermanentOnBattlefield(you, "Sorcery Mana Ignus")
        val golemId = d.putCardInHand(you, "Sorcery Mana Golem")

        d.submit(ActivateAbility(you, ignusId, abilityId)).error shouldBe null
        d.submit(CastSpell(you, golemId)).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Sorcery Mana Golem").shouldNotBeNull()
    }
})
