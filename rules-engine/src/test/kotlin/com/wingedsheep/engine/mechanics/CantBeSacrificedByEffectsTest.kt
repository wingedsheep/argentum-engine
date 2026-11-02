package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A permanent that "can't be sacrificed" (projected `AbilityFlag.CANT_BE_SACRIFICED`, from
 * `CantBeSacrificed` with the default scope — Zurgo, Thunder's Decree's Warrior tokens) survives
 * every sacrifice a *resolving* spell or ability performs, not only "sacrifice it":
 *
 *  - an edict ("each player sacrifices a creature") never offers it, so a player whose only
 *    candidates can't be sacrificed sacrifices nothing, and one with another candidate sacrifices
 *    that one without being asked;
 *  - "sacrifice a creature" by the resolving player likewise skips it;
 *  - "sacrifice all creatures" (a pipeline's sacrifice move) leaves it on the battlefield while
 *    everything else goes.
 */
class CantBeSacrificedByEffectsTest : FunSpec({

    val lock = card("Unsacrificeable Lock") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "Creatures you control can't be sacrificed."
        staticAbility { ability = CantBeSacrificed(GroupFilter.AllCreaturesYouControl) }
    }
    val bear = card("Unsac Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val trinket = card("Unsac Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    val eachEdict = card("Each Player Edict") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Each player sacrifices a creature."
        spell { effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each)) }
    }
    val ownEdict = card("Own Creature Or Artifact Edict") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice a creature or artifact."
        spell { effect = Effects.SacrificeOwn(GameObjectFilter.CreatureOrArtifact) }
    }
    val wipe = card("Sacrifice All Creatures Sorcery") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Each player sacrifices all creatures they control."
        spell { effect = Effects.SacrificeAll(GameObjectFilter.Creature) }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(lock, bear, trinket, eachEdict, ownEdict, wipe))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putLandOnBattlefield(me, "Swamp")
        return driver to me
    }

    fun GameTestDriver.cast(me: EntityId, name: String) {
        val spell = putCardInHand(me, name)
        submitSuccess(CastSpell(playerId = me, cardId = spell))
        bothPass()
    }

    test("an edict skips a creature that can't be sacrificed; other players still sacrifice") {
        val (driver, me) = newDriver()
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Unsacrificeable Lock")
        val mine = driver.putCreatureOnBattlefield(me, "Unsac Test Bear")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Unsac Test Bear")

        driver.cast(me, "Each Player Edict")

        withClue("no prompt: my only creature isn't a candidate") { driver.state.pendingDecision shouldBe null }
        driver.findPermanent(me, "Unsac Test Bear") shouldBe mine
        driver.getGraveyard(opponent) shouldContain theirs
    }

    test("'sacrifice a creature or artifact' takes the only candidate that can be sacrificed") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Unsacrificeable Lock")
        val creature = driver.putCreatureOnBattlefield(me, "Unsac Test Bear")
        val artifact = driver.putPermanentOnBattlefield(me, "Unsac Test Trinket")

        driver.cast(me, "Own Creature Or Artifact Edict")

        withClue("the creature isn't on offer, so the artifact goes without a choice") {
            driver.state.pendingDecision shouldBe null
        }
        driver.getGraveyard(me) shouldContain artifact
        driver.getGraveyard(me) shouldNotContain creature
        driver.findPermanent(me, "Unsac Test Bear") shouldBe creature
    }

    test("'sacrifice all creatures' leaves a creature that can't be sacrificed on the battlefield") {
        val (driver, me) = newDriver()
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Unsacrificeable Lock")
        val mine = driver.putCreatureOnBattlefield(me, "Unsac Test Bear")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Unsac Test Bear")

        driver.cast(me, "Sacrifice All Creatures Sorcery")

        driver.findPermanent(me, "Unsac Test Bear") shouldBe mine
        driver.getGraveyard(me) shouldNotContain mine
        driver.getGraveyard(opponent) shouldContain theirs
    }

    test("the protection ends with its source: the same sacrifice then takes the creature") {
        val (driver, me) = newDriver()
        val lockId = driver.putPermanentOnBattlefield(me, "Unsacrificeable Lock")
        val mine = driver.putCreatureOnBattlefield(me, "Unsac Test Bear")
        driver.moveToGraveyard(lockId)

        driver.cast(me, "Sacrifice All Creatures Sorcery")

        driver.getGraveyard(me) shouldContain mine
    }
})
