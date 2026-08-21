package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.ValgavothTerrorEater
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Valgavoth, Terror Eater (Duskmourn #120):
 *   Flying, lifelink
 *   Ward—Sacrifice three nonland permanents.
 *   If a card you didn't control would be put into an opponent's graveyard from anywhere,
 *     exile it instead.
 *   During your turn, you may play cards exiled with Valgavoth. If you cast a spell this way,
 *     pay life equal to its mana value rather than pay its mana cost.
 *
 * Exercises the three new engine capabilities: counted ward-sacrifice, the linked-exile
 * graveyard replacement, and the play-from-linked-exile permission with the life-equal-to-mana
 * value alternative cost (plus land plays from that exile pile).
 */
class ValgavothTerrorEaterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ValgavothTerrorEater))
        return driver
    }

    fun linkedExileOf(driver: GameTestDriver, sourceId: EntityId): List<EntityId> =
        driver.state.getEntity(sourceId)?.get<LinkedExileComponent>()?.exiledIds ?: emptyList()

    // --- Replacement: exile cards bound for an opponent's graveyard, linked to Valgavoth ---

    test("an opponent's creature that dies is exiled and linked to Valgavoth") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val valgavoth = driver.putCreatureOnBattlefield(you, "Valgavoth, Terror Eater")
        val victim = driver.putCreatureOnBattlefield(opp, "Grizzly Bears") // 2/2

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // bolt resolves, the 2/2 dies into the opponent's graveyard

        driver.getGraveyard(opp) shouldNotContain victim
        driver.getExile(opp) shouldContain victim
        linkedExileOf(driver, valgavoth) shouldContain victim
    }

    test("a creature that trades with Valgavoth is still exiled, but is not linked to the dead Valgavoth") {
        // CR 704.3 performs every applicable state-based action simultaneously as a single event,
        // and a replacement applies as that event is about to happen (CR 614.1) — so Valgavoth
        // still shields a creature dying alongside it. A deathtouch attacker that Valgavoth blocks
        // is the cheap way to kill a 9/9 in the same combat-damage check that kills the attacker
        // (it has to block rather than be blocked: Valgavoth flies, so a ground Rat can't block it).
        //
        // The *link* is a separate question. A permanent that has left the battlefield has had its
        // LinkedExileComponent stripped, and re-attaching one would strand a stale exile list on a
        // card in the graveyard that nothing cleans up — `removeFromLinkedExiles` only walks the
        // battlefield. Coming back is a new object with no memory of its previous existence
        // (CR 400.7), so the exile must not stay castable off a Valgavoth that died with it.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val valgavoth = driver.putCreatureOnBattlefield(defender, "Valgavoth, Terror Eater")
        val rat = driver.putCreatureOnBattlefield(attacker, "Deathtouch Rat") // 1/1 deathtouch
        driver.removeSummoningSickness(rat)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(rat), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(valgavoth to listOf(rat))).isSuccess shouldBe true
        driver.passPriorityUntil(Step.END_COMBAT)

        // Deathtouch kills the 9/9 (CR 704.5h) while its 9 damage kills the 1/1 (CR 704.5g).
        driver.getGraveyard(defender) shouldContain valgavoth
        driver.getGraveyard(attacker) shouldNotContain rat
        driver.getExile(attacker) shouldContain rat
        linkedExileOf(driver, valgavoth) shouldNotContain rat
    }

    test("your own creature dying is unaffected (only an opponent's graveyard)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val valgavoth = driver.putCreatureOnBattlefield(you, "Valgavoth, Terror Eater")
        val mine = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(mine)).isSuccess shouldBe true
        driver.bothPass()

        driver.getGraveyard(you) shouldContain mine
        driver.getExile(you) shouldNotContain mine
        linkedExileOf(driver, valgavoth) shouldNotContain mine
    }

    test("a card you control but an opponent owns is not exiled when it dies") {
        // "a card you didn't control": a stolen permanent (owned by the opponent, controlled by
        // you) that dies goes to its owner's (the opponent's) graveyard normally — Valgavoth does
        // not exile it because you controlled it.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val valgavoth = driver.putCreatureOnBattlefield(you, "Valgavoth, Terror Eater")
        // A creature on your battlefield, but owned by the opponent.
        val stolen = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.replaceState(
            driver.state.updateEntity(stolen) { c ->
                val card = c.get<CardComponent>()!!
                c.with(card.copy(ownerId = opp))
            }
        )

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(stolen)).isSuccess shouldBe true
        driver.bothPass()

        // Goes to the opponent's graveyard (its owner), NOT exiled — you controlled it.
        driver.getGraveyard(opp) shouldContain stolen
        driver.getExile(opp) shouldNotContain stolen
        linkedExileOf(driver, valgavoth) shouldNotContain stolen
    }

    // --- Play from linked exile: pay life equal to mana value rather than mana cost ---

    test("you may cast a spell exiled with Valgavoth by paying life equal to its mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Valgavoth, Terror Eater")
        val victim = driver.putCreatureOnBattlefield(opp, "Grizzly Bears") // {1}{G}, mana value 2

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // Grizzly Bears dies → exiled with Valgavoth

        driver.getExile(opp) shouldContain victim

        // Cast it from Valgavoth's exile — no mana paid, but 2 life (its mana value).
        driver.getLifeTotal(you) shouldBe 20
        driver.submit(
            CastSpell(playerId = you, cardId = victim, paymentStrategy = PaymentStrategy.AutoPay)
        ).isSuccess shouldBe true
        driver.bothPass() // the creature spell resolves onto your battlefield

        driver.getLifeTotal(you) shouldBe 18
        driver.getController(victim) shouldBe you
        driver.findPermanent(you, "Grizzly Bears") shouldNotBe null
        driver.getExile(opp) shouldNotContain victim
    }

    test("you may play a land exiled with Valgavoth for free") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val valgavoth = driver.putCreatureOnBattlefield(you, "Valgavoth, Terror Eater")
        // An opponent-owned land sitting in exile, linked to Valgavoth (as if its land had died
        // into the opponent's graveyard and been redirected).
        val land = driver.putCardInExile(opp, "Forest")
        driver.replaceState(
            driver.state.updateEntity(valgavoth) { c ->
                c.with(LinkedExileComponent(listOf(land)))
            }
        )

        driver.getLifeTotal(you) shouldBe 20
        driver.playLand(you, land).isSuccess shouldBe true

        driver.findPermanent(you, "Forest") shouldNotBe null
        driver.getController(land) shouldBe you
        driver.getLifeTotal(you) shouldBe 20 // lands cost no life
        linkedExileOf(driver, valgavoth) shouldNotContain land
    }

    // --- Ward—Sacrifice three nonland permanents ---

    test("ward counters the spell when the caster cannot sacrifice three nonland permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!       // casts the removal
        val defender = driver.getOpponent(caster) // controls Valgavoth

        val valgavoth = driver.putCreatureOnBattlefield(defender, "Valgavoth, Terror Eater")
        // The caster controls only two nonland permanents — not enough for Ward (3).
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(valgavoth)))
        repeat(4) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // Can't pay 3 → ward auto-counters with no prompt; Valgavoth survives.
        driver.pendingDecision shouldBe null
        driver.findPermanent(defender, "Valgavoth, Terror Eater") shouldNotBe null
        // The countered bolt heads to the caster's graveyard — Valgavoth exiles it instead.
        driver.getExile(caster) shouldContain bolt
    }

    test("ward is satisfied by sacrificing three nonland permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)

        val valgavoth = driver.putCreatureOnBattlefield(defender, "Valgavoth, Terror Eater")
        val fodder = listOf(
            driver.putCreatureOnBattlefield(caster, "Grizzly Bears"),
            driver.putCreatureOnBattlefield(caster, "Grizzly Bears"),
            driver.putCreatureOnBattlefield(caster, "Grizzly Bears"),
        )

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(valgavoth)))

        // Ward resolves → caster is prompted to sacrifice three nonland permanents.
        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe caster
        fodder.forEach { decision.options shouldContain it }

        driver.submitDecision(caster, CardsSelectedResponse(decision.id, fodder))
        repeat(6) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // All three were sacrificed; the bolt resolved (Valgavoth, a 9/9, survived).
        fodder.forEach { driver.getController(it) shouldNotBe caster }
        driver.findPermanent(defender, "Valgavoth, Terror Eater") shouldNotBe null
    }
})
