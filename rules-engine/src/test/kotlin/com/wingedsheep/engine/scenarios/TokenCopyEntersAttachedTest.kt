package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CreateTokenCopyOfTarget(attachedTo = …)` — "create a token that's a copy of that permanent
 * attached to that creature". The host is prescribed, so there is no choice, and legality follows
 * the entering-attached rules rather than targeting:
 *  - CR 303.4i: an Aura copy that can't legally enchant the host (or whose host is gone) isn't created;
 *  - CR 301.5e: an Equipment copy that can't legally equip the host is created unattached;
 *  - CR 303.4h: a copy that is neither an Aura nor an Equipment enters unattached.
 */
class TokenCopyEntersAttachedTest : FunSpec({

    val creatureAura = card("Test Might") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nEnchanted creature gets +1/+2."
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = ModifyStats(1, 2) }
    }

    val landAura = card("Test Land Blessing") {
        manaCost = "{G}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant land"
        auraTarget = TargetObject(filter = TargetFilter.Land)
    }

    val blade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature gets +2/+0.\nEquip {1}"
        staticAbility { ability = ModifyStats(2, 0) }
        equipAbility("{1}")
    }

    val totem = card("Test Totem") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    // A shroud creature can't be targeted even by its controller; it names itself as the host.
    val shroudedGrafter = card("Test Shrouded Grafter") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Shroud\n{0}: Create a token that's a copy of target permanent attached to this creature."
        keywords(Keyword.SHROUD)
        activatedAbility {
            cost = Costs.Free
            val original = target(TargetObject(filter = TargetFilter.Permanent))
            effect = Effects.CreateTokenCopyOfTarget(target = original, attachedTo = EffectTarget.Self)
        }
    }

    // "Create a token that's a copy of target permanent attached to target permanent."
    val graft = card("Test Graft") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Create a token that's a copy of target permanent attached to target permanent."
        spell {
            val original = target(TargetObject(filter = TargetFilter.Permanent))
            val host = target(TargetObject(filter = TargetFilter.Permanent))
            effect = Effects.CreateTokenCopyOfTarget(target = original, attachedTo = host)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(creatureAura, landAura, blade, totem, shroudedGrafter, graft))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolve(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun cast(driver: GameTestDriver, original: EntityId, host: EntityId) {
        val active = driver.activePlayer!!
        val graftCard = driver.putCardInHand(active, "Test Graft")
        driver.castSpell(active, graftCard, targets = listOf(original, host)).error shouldBe null
        resolve(driver)
    }

    fun tokens(driver: GameTestDriver, name: String): List<EntityId> =
        driver.state.getZone(driver.activePlayer!!, Zone.BATTLEFIELD).filter { id ->
            val c = driver.state.getEntity(id) ?: return@filter false
            c.get<CardComponent>()?.name == name && c.get<TokenComponent>() != null
        }

    // The copied Aura must itself be attached somewhere, or state-based actions bin it first.
    fun attach(driver: GameTestDriver, attachment: EntityId, host: EntityId) {
        var s = driver.state.updateEntity(attachment) { it.with(AttachedToComponent(host)) }
        s = s.updateEntity(host) { c ->
            c.with(AttachmentsComponent(c.get<AttachmentsComponent>()?.attachedIds.orEmpty() + attachment))
        }
        driver.replaceState(s)
    }

    fun attachedAura(driver: GameTestDriver, name: String, hostName: String): EntityId {
        val active = driver.activePlayer!!
        val host = driver.putPermanentOnBattlefield(active, hostName)
        val aura = driver.putPermanentOnBattlefield(active, name)
        attach(driver, aura, host)
        return aura
    }

    fun attachedTo(driver: GameTestDriver, id: EntityId): EntityId? =
        driver.state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    test("an Aura copy enters attached to the prescribed creature, with no host choice") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val aura = attachedAura(driver, "Test Might", "Grizzly Bears")
        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")

        cast(driver, aura, bear)

        driver.isPaused shouldBe false
        val token = tokens(driver, "Test Might").single()
        attachedTo(driver, token) shouldBe bear
        driver.state.getEntity(bear)!!.get<AttachmentsComponent>()!!.attachedIds shouldContain token
        driver.events.filterIsInstance<PermanentAttachedEvent>()
            .any { it.attachmentId == token && it.attachedToId == bear } shouldBe true
        // The token's static ability reaches its host (+1/+2 on a 2/2).
        driver.state.projectedState.getPower(bear) shouldBe 3
        driver.state.projectedState.getToughness(bear) shouldBe 4
    }

    test("entering attached isn't targeting: an Aura copy attaches to a shroud creature") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val aura = attachedAura(driver, "Test Might", "Grizzly Bears")
        val grafter = driver.putCreatureOnBattlefield(active, "Test Shrouded Grafter")
        val abilityId = driver.cardRegistry.requireCard("Test Shrouded Grafter").activatedAbilities[0].id

        driver.submitSuccess(
            ActivateAbility(
                playerId = active, sourceId = grafter, abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(aura))
            )
        )
        resolve(driver)

        attachedTo(driver, tokens(driver, "Test Might").single()) shouldBe grafter
    }

    test("CR 303.4i: an Aura copy that can't enchant the prescribed host isn't created") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val landAuraId = attachedAura(driver, "Test Land Blessing", "Forest")
        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")

        cast(driver, landAuraId, bear)

        driver.state.getBattlefield() shouldContain landAuraId
        tokens(driver, "Test Land Blessing") shouldHaveSize 0
        driver.state.getEntity(bear)!!.get<AttachmentsComponent>()?.attachedIds.orEmpty() shouldHaveSize 0
    }

    test("CR 303.4i: an Aura copy whose prescribed host has left the battlefield isn't created") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val aura = attachedAura(driver, "Test Might", "Grizzly Bears")
        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val graftCard = driver.putCardInHand(active, "Test Graft")
        driver.castSpell(active, graftCard, targets = listOf(aura, bear)).error shouldBe null
        driver.moveToGraveyard(bear)
        resolve(driver)

        driver.state.getBattlefield() shouldContain aura
        tokens(driver, "Test Might") shouldHaveSize 0
    }

    test("an Equipment copy enters attached to the prescribed creature") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val bladeId = driver.putPermanentOnBattlefield(active, "Test Blade")
        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")

        cast(driver, bladeId, bear)

        val token = tokens(driver, "Test Blade").single()
        attachedTo(driver, token) shouldBe bear
        driver.state.projectedState.getPower(bear) shouldBe 4
    }

    test("CR 301.5e: an Equipment copy that can't equip the host is created unattached") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val bladeId = driver.putPermanentOnBattlefield(active, "Test Blade")
        val totemId = driver.putPermanentOnBattlefield(active, "Test Totem")

        cast(driver, bladeId, totemId)

        val token = tokens(driver, "Test Blade").single()
        attachedTo(driver, token) shouldBe null
        driver.state.getEntity(totemId)!!.get<AttachmentsComponent>()?.attachedIds.orEmpty() shouldHaveSize 0
    }

    test("CR 303.4h: a copy that is neither an Aura nor an Equipment enters unattached") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val totemId = driver.putPermanentOnBattlefield(active, "Test Totem")
        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")

        cast(driver, totemId, bear)

        val token = tokens(driver, "Test Totem").single()
        token shouldNotBe null
        attachedTo(driver, token) shouldBe null
    }
})
