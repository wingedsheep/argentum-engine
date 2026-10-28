package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerHexproofFromComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Hexproof from [quality]" granted by an effect — `Effects.GrantHexproofFrom(scope, target)`
 * (CR 702.11d; Veil of Summer). On a player it rides a `PlayerHexproofFromComponent` the targeting
 * rules read against each spell's or ability's source; on a permanent it is the projected
 * `HEXPROOF_FROM_*` keyword a printed hexproof-from carries.
 */
class HexproofFromGrantTest : FunSpec({

    val blueOrBlack = ProtectionScope.Colors(setOf(Color.BLUE, Color.BLACK))

    val veil = card("Test Veil of Blue and Black") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell { effect = Effects.GrantHexproofFrom(blueOrBlack) }
    }

    val lastingVeil = card("Test Lasting Veil of Black") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell { effect = Effects.GrantHexproofFrom(ProtectionScope.Color(Color.BLACK), duration = Duration.Permanent) }
    }

    val creatureVeil = card("Test Creature Veil") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantHexproofFrom(blueOrBlack, creature)
        }
    }

    val bluePing = card("Test Blue Ping") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.LoseLife(1, target(Targets.Player)) }
    }

    val bluePinger = card("Test Blue Pinger") {
        manaCost = "{U}"
        typeLine = "Creature — Wizard"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Free
            effect = Effects.LoseLife(1, target(Targets.Player))
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(veil, lastingVeil, creatureVeil, bluePing, bluePinger))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castVeil(player: com.wingedsheep.sdk.model.EntityId, name: String) {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.GREEN, 1)
        castSpell(player, spell).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("an opponent's spell of a named color can't target you; other colors and your own spells can") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.castVeil(p1, "Test Veil of Blue and Black")

        driver.passPriority(p1)
        val ping = driver.putCardInHand(p2, "Test Blue Ping")
        driver.giveMana(p2, Color.BLUE, 1)
        withClue("an opponent's blue spell can't target a player with hexproof from blue") {
            driver.castSpellWithTargets(p2, ping, listOf(ChosenTarget.Player(p1))).error shouldNotBe null
        }
        val bolt = driver.putCardInHand(p2, "Lightning Bolt")
        driver.giveMana(p2, Color.RED, 1)
        withClue("a red spell is not blue or black, so it can still target the player") {
            driver.castSpellWithTargets(p2, bolt, listOf(ChosenTarget.Player(p1))).error shouldBe null
        }
        driver.bothPass()
        driver.assertLifeTotal(p1, 17)

        val ownPing = driver.putCardInHand(p1, "Test Blue Ping")
        driver.giveMana(p1, Color.BLUE, 1)
        withClue("hexproof never stops the player's own spells (CR 702.11c)") {
            driver.castSpellWithTargets(p1, ownPing, listOf(ChosenTarget.Player(p1))).error shouldBe null
        }
    }

    test("an opponent's ability from a source of the named color can't target you") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val pinger = driver.putCreatureOnBattlefield(p2, "Test Blue Pinger")
        driver.castVeil(p1, "Test Veil of Blue and Black")

        driver.passPriority(p1)
        val abilityId = bluePinger.script.activatedAbilities.single().id
        withClue("an ability from a blue source can't target the player") {
            driver.submit(ActivateAbility(p2, pinger, abilityId, listOf(ChosenTarget.Player(p1)))).error shouldNotBe null
        }
        withClue("the same ability can still target its own controller") {
            driver.submit(ActivateAbility(p2, pinger, abilityId, listOf(ChosenTarget.Player(p2)))).error shouldBe null
        }
    }

    test("a grant made in response makes an already-targeted spell fizzle on resolution") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        val ping = driver.putCardInHand(p1, "Test Blue Ping")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpellWithTargets(p1, ping, listOf(ChosenTarget.Player(p2))).error shouldBe null
        driver.passPriority(p1)
        val response = driver.putCardInHand(p2, "Test Veil of Blue and Black")
        driver.giveMana(p2, Color.GREEN, 1)
        driver.castSpell(p2, response).outcome shouldBe Outcome.Done
        driver.bothPass() // veil resolves
        driver.bothPass() // ping tries to resolve
        withClue("the blue ping's only target became illegal, so it doesn't resolve (CR 608.2b)") {
            driver.assertLifeTotal(p2, 20)
        }
    }

    test("a permanent gains the hexproof-from keywords: matching removal can't target it, others can") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")

        val veilSpell = driver.putCardInHand(p1, "Test Creature Veil")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.castSpellWithTargets(p1, veilSpell, listOf(ChosenTarget.Permanent(courser))).error shouldBe null
        driver.bothPass()

        withClue("the creature carries the printed-style keywords") {
            driver.state.projectedState.hasKeyword(courser, "HEXPROOF_FROM_BLUE") shouldBe true
            driver.state.projectedState.hasKeyword(courser, "HEXPROOF_FROM_BLACK") shouldBe true
        }

        driver.passPriority(p1)
        val blade = driver.putCardInHand(p2, "Doom Blade")
        driver.giveMana(p2, Color.BLACK, 2)
        withClue("an opponent's black Doom Blade can't target it") {
            driver.castSpellWithTargets(p2, blade, listOf(ChosenTarget.Permanent(courser))).error shouldNotBe null
        }
        val bolt = driver.putCardInHand(p2, "Lightning Bolt")
        driver.giveMana(p2, Color.RED, 1)
        withClue("a red Lightning Bolt still can") {
            driver.castSpellWithTargets(p2, bolt, listOf(ChosenTarget.Permanent(courser))).error shouldBe null
        }
    }

    test("an end-of-turn grant expires at cleanup while a lasting one stays") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.castVeil(p1, "Test Lasting Veil of Black")
        driver.castVeil(p1, "Test Veil of Blue and Black")

        driver.state.getEntity(p1)?.get<PlayerHexproofFromComponent>()?.grants?.size shouldBe 2

        driver.passPriorityUntil(Step.UPKEEP, activePlayer = p2)
        val grants = driver.state.getEntity(p1)?.get<PlayerHexproofFromComponent>()?.grants
        withClue("only the lasting hexproof from black survives the turn") {
            grants shouldBe listOf(
                PlayerHexproofFromComponent.Grant(ProtectionScope.Color(Color.BLACK), PlayerEffectRemoval.Permanent)
            )
        }
    }

    test("an end-of-turn grant alone leaves no component behind") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.castVeil(p1, "Test Veil of Blue and Black")

        driver.passPriorityUntil(Step.UPKEEP, activePlayer = p2)
        driver.state.getEntity(p1)?.get<PlayerHexproofFromComponent>() shouldBe null
    }
})
