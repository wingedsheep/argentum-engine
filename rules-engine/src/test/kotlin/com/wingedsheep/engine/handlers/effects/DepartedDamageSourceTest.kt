package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.damage.DamageReplacementChain
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.captureLastKnown
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A named, non-targeted damage source ("it deals that much damage" — Wrathful Red Dragon,
 * Warstorm Surge) that has left the battlefield before its instruction resolves still deals the
 * damage, using its last-known information (CR 608.2h): the damage happens, and the departed
 * source's lifelink and deathtouch as it last existed on the battlefield still apply.
 */
class DepartedDamageSourceTest : FunSpec({

    // "Whenever a creature you control is dealt damage, it deals that much damage to any target."
    val ReflectionField = card("Reflection Field") {
        manaCost = "{2}{R}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control is dealt damage, it deals that much damage to any target."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).isDealtDamage()
            val t = target(Targets.Any)
            effect = Effects.DealDamage(
                amount = DynamicAmounts.triggerDamageAmount(),
                target = t,
                damageSource = EffectTarget.TriggeringEntity
            )
        }
    }

    val LifelinkMite = card("Lifelink Mite") {
        manaCost = "{W}"
        typeLine = "Creature — Insect"
        power = 1
        toughness = 1
        keywords(Keyword.LIFELINK)
    }

    val DeathtouchMite = card("Deathtouch Mite") {
        manaCost = "{B}"
        typeLine = "Creature — Insect"
        power = 1
        toughness = 1
        keywords(Keyword.DEATHTOUCH)
    }

    val BigWall = card("Big Wall") {
        manaCost = "{3}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReflectionField, LifelinkMite, DeathtouchMite, BigWall))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Bolt [victim] (3 damage, lethal for a 1/1), then aim the reflected damage at [reflectTarget]. */
    fun boltAndReflect(driver: GameTestDriver, victim: EntityId, reflectTarget: EntityId) {
        val p1 = driver.player1
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        var guard = 0
        while (guard++ < 16) {
            when (val decision = driver.state.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(decision.playerId, listOf(reflectTarget))
                null -> {
                    if (driver.state.stack.isEmpty()) return
                    driver.bothPass()
                }
                else -> driver.autoResolveDecision()
            }
        }
    }

    test("a creature killed by the damage still deals the reflected damage") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Reflection Field")
        val mite = driver.putCreatureOnBattlefield(driver.player1, "Deathtouch Mite")

        boltAndReflect(driver, victim = mite, reflectTarget = driver.player2)

        driver.getGraveyardCardNames(driver.player1) shouldContain "Deathtouch Mite"
        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("a departed source's lifelink, as it last existed, gains its controller life") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Reflection Field")
        val mite = driver.putCreatureOnBattlefield(driver.player1, "Lifelink Mite")

        boltAndReflect(driver, victim = mite, reflectTarget = driver.player2)

        driver.getGraveyardCardNames(driver.player1) shouldContain "Lifelink Mite"
        driver.getLifeTotal(driver.player2) shouldBe 17
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("a departed source's deathtouch, as it last existed, destroys the creature it damages") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Reflection Field")
        val mite = driver.putCreatureOnBattlefield(driver.player1, "Deathtouch Mite")
        val wall = driver.putCreatureOnBattlefield(driver.player2, "Big Wall")

        boltAndReflect(driver, victim = mite, reflectTarget = wall)

        driver.getGraveyardCardNames(driver.player2) shouldContain "Big Wall"
    }

    val fanatic = card("Departed Fanatic") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.SacrificeSelf
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val plans = card("Departed Color Ward") {
        typeLine = "Enchantment"
        replacementEffect(PreventDamage(appliesTo = EventPattern.DamageEvent(
            recipient = Recipient.AnyCreature,
            source = GameObjectFilter.Creature.sharingColorWithRecipient()
        )))
    }

    test("Well-Laid Plans pattern prevents a sacrificed red creature damaging another red creature") {
        val d = newDriver()
        d.registerCards(listOf(fanatic, plans))
        d.putPermanentOnBattlefield(d.player1, plans.name)
        val source = d.putCreatureOnBattlefield(d.player1, fanatic.name)
        val recipient = d.putCreatureOnBattlefield(d.player2, "Goblin Guide")
        d.submit(ActivateAbility(d.player1, source, fanatic.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(recipient)))).error shouldBe null
        d.bothPass().error shouldBe null
        d.getGraveyardCardNames(d.player1) shouldContain fanatic.name
        (d.state.getEntity(recipient)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (d.state.getEntity(recipient) != null) shouldBe true
    }

    test("recipient color filtering reads departed colors instead of a newer green incarnation") {
        val d = newDriver()
        d.registerCard(plans)
        d.putPermanentOnBattlefield(d.player1, plans.name)
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val recipient = d.putCreatureOnBattlefield(d.player2, "Goblin Guide")
        val captured = captureLastKnown(d.state, source)
        val snapshot = captured.copy(colors = setOf("RED"),
            objectRef = captured.objectRef!!.copy(generation = captured.objectRef!!.generation - 1))
        val live = DamageReplacementChain.deal(d.zones, d.state, listOf(
            DamageReplacementChain.Instance(source, recipient, 1)
        ))
        live.state.getEntity(recipient)?.get<DamageComponent>()?.amount shouldBe 1
        val result = DamageReplacementChain.deal(d.zones, d.state, listOf(
            DamageReplacementChain.Instance(source, recipient, 1, sourceSnapshot = snapshot)
        ))
        result.error shouldBe null
        (result.state.getEntity(recipient)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("snapshot recipient color predicates compose and distinguish a returned object from itself") {
        val d = newDriver()
        val source = d.putCreatureOnBattlefield(d.player1, "Goblin Guide")
        val snapshot = captureLastKnown(d.state, source)
        val predicate = CardPredicate.SharesColorWithRecipient
        val context = PredicateContext(controllerId = d.player1, recipientId = source)
        val evaluator = d.zones.predicateEvaluator
        fun matches(p: CardPredicate, old: Boolean = true, colors: Set<String>? = setOf("RED")) =
            evaluator.matchesSnapshot(d.state, snapshot.copy(
                colors = colors,
                objectRef = if (old) snapshot.objectRef!!.copy(generation = snapshot.objectRef!!.generation - 1) else snapshot.objectRef
            ), GameObjectFilter(cardPredicates = listOf(p)), context)
        matches(predicate, old = false) shouldBe false
        matches(predicate) shouldBe true
        matches(CardPredicate.Not(predicate)) shouldBe false
        matches(CardPredicate.And(listOf(CardPredicate.IsCreature, predicate))) shouldBe true
        matches(CardPredicate.Or(listOf(CardPredicate.IsLand, predicate))) shouldBe true
        matches(CardPredicate.Not(predicate), colors = null) shouldBe false
        matches(predicate, colors = emptySet()) shouldBe false
        evaluator.matchesSnapshot(d.state, snapshot.copy(objectRef = snapshot.objectRef!!.copy(generation = -1)),
            GameObjectFilter(anyOf = listOf(GameObjectFilter.Land, GameObjectFilter.Creature.sharingColorWithRecipient())), context) shouldBe true
    }

    test("snapshot damage-source mana parity and chosen subtype use frozen characteristics") {
        val d = newDriver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val snapshot = captureLastKnown(d.state, source).copy(manaValue = 3, subtypes = setOf("Goblin"))
        val context = PredicateContext(controllerId = d.player1, sourceId = source,
            lastKnownSourceSnapshot = snapshot.copy(textChoices = mapOf(com.wingedsheep.sdk.scripting.ChoiceSlot.CREATURE_TYPE to "Goblin")))
        fun matches(p: CardPredicate, faceDown: Boolean = false) = d.zones.predicateEvaluator.matchesSnapshot(
            d.state, snapshot.copy(wasFaceDown = faceDown), GameObjectFilter(cardPredicates = listOf(p)), context)
        matches(CardPredicate.ManaValueIsOdd) shouldBe true
        matches(CardPredicate.ManaValueIsEven) shouldBe false
        matches(CardPredicate.ManaValueIsOdd, faceDown = true) shouldBe false
        matches(CardPredicate.ManaValueIsEven, faceDown = true) shouldBe true
        matches(CardPredicate.HasChosenSubtype) shouldBe true
    }


    test("departed enchanted source matching uses captured attachments") {
        val d = newDriver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val snapshot = captureLastKnown(d.state, source)
        val context = PredicateContext(controllerId = d.player1)
        val enchanted = snapshot.copy(attachments = listOf(
            com.wingedsheep.engine.state.components.stack.AttachmentLki(source, d.player1, setOf("Aura"))
        ))
        d.zones.predicateEvaluator.matchesSnapshot(d.state, enchanted,
            GameObjectFilter.Creature.enchanted(), context) shouldBe true
        d.zones.predicateEvaluator.matchesSnapshot(d.state, snapshot,
            GameObjectFilter.Creature.enchanted(), context) shouldBe false
    }

})
