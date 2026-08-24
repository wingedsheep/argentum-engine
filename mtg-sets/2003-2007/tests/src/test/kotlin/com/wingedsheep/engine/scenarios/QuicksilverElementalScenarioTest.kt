package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.QuicksilverElemental
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SpikeshotGoblin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Quicksilver Elemental (MRD #47) — "{U}: This creature gains all activated abilities of target
 * creature until end of turn. You may spend blue mana as though it were mana of any color to pay
 * the activation costs of this creature's abilities."
 *
 * What the rulings actually require, and what each test pins:
 *  - "The granted abilities effectively use 'this permanent'" — a gained `{R}, {T}: deals damage
 *    equal to its power` taps the *Elemental* and deals the *Elemental's* 3, not the Goblin's 1.
 *  - The gain is snapshotted on resolution (the Havengul Lich wording), so the donor dying
 *    afterwards doesn't take the ability back.
 *  - Mana abilities count as activated abilities, so a gained "{T}: Add {G}" really produces mana.
 *  - "You can activate the ability more than once, collecting abilities from multiple creatures" —
 *    grants accumulate rather than replace.
 *  - The blue substitution is "as though it were mana of any **color**", *not* "mana of any type":
 *    blue pays a gained `{R}`, and green does not.
 *  - "Until end of turn" — the grants are gone next turn.
 */
class QuicksilverElementalScenarioTest : FunSpec({

    val gainAbility = QuicksilverElemental.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + QuicksilverElemental + SpikeshotGoblin)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** The Elemental on the battlefield, ready to use `{T}` abilities. */
    fun GameTestDriver.elemental(): EntityId =
        putCreatureOnBattlefield(player1, "Quicksilver Elemental").also { removeSummoningSickness(it) }

    /** Pay {U} and resolve "gains all activated abilities of [donor]". */
    fun GameTestDriver.gainAbilitiesOf(elemental: EntityId, donor: EntityId) {
        giveMana(player1, Color.BLUE, 1)
        submit(
            ActivateAbility(player1, elemental, gainAbility, targets = listOf(ChosenTarget.Permanent(donor)))
        ).error shouldBe null
        bothPass()
    }

    /** The ids of the activated abilities currently granted to [entityId]. */
    fun GameTestDriver.gainedAbilityIds(entityId: EntityId): List<AbilityId> =
        state.grantedActivatedAbilities.filter { it.entityId == entityId }.map { it.ability.id }

    test("a gained ability binds to the Elemental: it taps the Elemental and uses the Elemental's power") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val elemental = d.elemental()
        val goblin = d.putCreatureOnBattlefield(d.player2, "Spikeshot Goblin") // 1/2, {R}, {T}
        d.gainAbilitiesOf(elemental, goblin)

        val gained = d.gainedAbilityIds(elemental).single()
        withClue("the gain is offered as a legal action, not just accepted by the handler — the " +
            "client and the AI only ever see what the enumerator produces") {
            d.giveMana(d.player1, Color.RED, 1)
            d.legalActions(d.player1)
                .map { it.action }
                .filterIsInstance<ActivateAbility>()
                .any { it.sourceId == elemental && it.abilityId == gained } shouldBe true
        }
        d.submit(
            ActivateAbility(d.player1, elemental, gained, targets = listOf(ChosenTarget.Player(opponent)))
        ).error shouldBe null
        d.bothPass()

        withClue("\"deals damage equal to its power\" is the Elemental's 3, not the Goblin's 1") {
            d.getLifeTotal(opponent) shouldBe 17
        }
        withClue("the {T} in the copied cost taps the permanent that gained it") {
            d.isTapped(elemental) shouldBe true
            d.isTapped(goblin) shouldBe false
        }
    }

    test("blue mana pays a gained {R}, but green mana does not") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val elemental = d.elemental()
        val goblin = d.putCreatureOnBattlefield(d.player2, "Spikeshot Goblin")
        d.gainAbilitiesOf(elemental, goblin)
        val gained = d.gainedAbilityIds(elemental).single()

        withClue("green can't pay a gained {R}: the relaxation widens colors via blue, not to generic") {
            d.giveMana(d.player1, Color.GREEN, 1)
            d.submit(
                ActivateAbility(d.player1, elemental, gained, targets = listOf(ChosenTarget.Player(opponent)))
            ).error shouldNotBe null
            d.stackSize shouldBe 0
        }

        withClue("blue does, because \"you may spend blue mana as though it were mana of any color\"") {
            d.giveMana(d.player1, Color.BLUE, 1)
            d.submit(
                ActivateAbility(d.player1, elemental, gained, targets = listOf(ChosenTarget.Player(opponent)))
            ).error shouldBe null
            d.bothPass()
            d.getLifeTotal(opponent) shouldBe 17
        }
    }

    test("the gain is snapshotted: the donor dying afterwards doesn't take the ability back") {
        val d = driver()
        val elemental = d.elemental()
        val goblin = d.putCreatureOnBattlefield(d.player2, "Spikeshot Goblin")
        d.gainAbilitiesOf(elemental, goblin)
        d.gainedAbilityIds(elemental).size shouldBe 1

        d.moveToGraveyard(goblin)

        withClue("\"gains the activated abilities of the card as it existed\" — the grant is a snapshot") {
            d.gainedAbilityIds(elemental).size shouldBe 1
        }
    }

    test("a gained mana ability really produces mana") {
        val d = driver()
        val elemental = d.elemental()
        val elves = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves") // {T}: Add {G}
        d.gainAbilitiesOf(elemental, elves)

        val gained = d.gainedAbilityIds(elemental).single()
        d.submit(ActivateAbility(d.player1, elemental, gained)).error shouldBe null

        withClue("mana abilities are activated abilities, so \"all activated abilities\" includes them") {
            d.state.getEntity(d.player1)?.get<ManaPoolComponent>()?.green shouldBe 1
            d.isTapped(elemental) shouldBe true
        }
    }

    test("activating twice collects from both creatures, and everything expires at end of turn") {
        val d = driver()
        val elemental = d.elemental()
        val goblin = d.putCreatureOnBattlefield(d.player2, "Spikeshot Goblin")
        val elves = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        d.gainAbilitiesOf(elemental, goblin)
        d.gainAbilitiesOf(elemental, elves)

        withClue("grants accumulate — \"you can activate the ability more than once\"") {
            d.gainedAbilityIds(elemental).size shouldBe 2
            d.gainedAbilityIds(elemental).distinct().size shouldBe 2
        }

        d.passPriorityUntil(Step.UPKEEP)
        withClue("until end of turn") {
            d.gainedAbilityIds(elemental) shouldBe emptyList()
        }
    }
})
