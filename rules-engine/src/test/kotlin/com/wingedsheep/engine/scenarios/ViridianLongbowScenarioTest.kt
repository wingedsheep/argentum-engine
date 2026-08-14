package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.ViridianLongbow
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Viridian Longbow — {1} Artifact — Equipment (Mirrodin #270)
 *
 * "Equipped creature has "{T}: This creature deals 1 damage to any target."
 *  Equip {3}"
 *
 * The whole card is a [GrantActivatedAbility] static, so what needs proving is that the granted
 * ability behaves as if printed on the *host* (CR 113.7):
 *  - the `{T}` cost taps the equipped creature, not the Longbow;
 *  - the equipped creature is the source of the damage (the deathtouch combo);
 *  - the ability goes away when the Longbow is no longer attached.
 */
class ViridianLongbowScenarioTest : FunSpec({

    // Equip {3} is the Longbow's only printed activated ability.
    fun equipAbilityId() = ViridianLongbow.activatedAbilities.single().id

    // The granted pinger lives inside the GrantActivatedAbility static.
    fun grantedAbilityId() =
        ViridianLongbow.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ViridianLongbow)
        return driver
    }

    /** Puts the Longbow and [creatureName] on p1's battlefield and equips them. */
    fun GameTestDriver.equipTo(p1: EntityId, creatureName: String): Pair<EntityId, EntityId> {
        val longbow = putPermanentOnBattlefield(p1, "Viridian Longbow")
        val creature = putCreatureOnBattlefield(p1, creatureName)
        // The granted ability costs {T}, so the host must be able to tap (CR 302.6).
        removeSummoningSickness(creature)
        passPriorityUntil(Step.PRECOMBAT_MAIN)

        giveColorlessMana(p1, 3)
        submit(
            ActivateAbility(
                playerId = p1,
                sourceId = longbow,
                abilityId = equipAbilityId(),
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        ).isSuccess shouldBe true
        bothPass()

        state.getEntity(longbow)?.get<AttachedToComponent>()?.targetId shouldBe creature
        return longbow to creature
    }

    test("granted {T} ability pings a creature and taps the host, not the Equipment") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)

        val (longbow, courser) = driver.equipTo(p1, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 1/1

        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = courser,
                abilityId = grantedAbilityId(),
                targets = listOf(ChosenTarget.Permanent(lions))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // 1 damage is lethal to a 1/1, so the damage demonstrably landed.
        driver.findPermanent(opponent, "Savannah Lions") shouldBe null

        // The {T} tapped the equipped creature (Self = host), and left the Longbow untapped.
        driver.state.getEntity(courser)?.get<TappedComponent>() shouldNotBe null
        driver.state.getEntity(longbow)?.get<TappedComponent>() shouldBe null
    }

    test("granted {T} ability can shoot a player") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)

        val (_, courser) = driver.equipTo(p1, "Centaur Courser")

        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = courser,
                abilityId = grantedAbilityId(),
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("the equipped creature is the damage source, so its deathtouch makes 1 damage lethal") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)

        val (_, rat) = driver.equipTo(p1, "Deathtouch Rat") // 1/1 deathtouch
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = rat,
                abilityId = grantedAbilityId(),
                targets = listOf(ChosenTarget.Permanent(courser))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // If the Longbow were the source the 3/3 would survive 1 damage. It doesn't:
        // the Rat is the source, and any nonzero damage from a deathtouch source is lethal.
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }

    test("unequipping removes the granted ability") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)

        val (longbow, courser) = driver.equipTo(p1, "Centaur Courser")

        // Longbow leaves the battlefield; the grant goes with it.
        driver.moveToGraveyard(longbow)

        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = courser,
                abilityId = grantedAbilityId(),
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe false
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
