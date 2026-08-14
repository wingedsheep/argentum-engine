package com.wingedsheep.engine.view

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The client paints a padlock on any permanent that won't untap (`untapRestriction.ts`). That badge
 * reads three things off [ClientCard]: [AbilityFlag.DOESNT_UNTAP] and
 * [AbilityFlag.CANT_BECOME_UNTAPPED] in `abilityFlags`, and [ClientCard.isExerted].
 *
 * The flags reach the client only because they ride the *projected* keyword set — the same set the
 * untap step itself gates on via `ProjectedState.doesntUntapDuringUntapStep`. That is what makes
 * the badge trustworthy and also what makes it fragile: a restriction implemented outside the layer
 * system (read directly by a manager, as the block restrictions are) would still stop the untap
 * while leaving the card visually indistinguishable from one that untaps normally. These tests pin
 * the contract for both the printed and the granted form, and for both viewers — an untap
 * restriction is public information, and the opponent's read of the board is exactly what it's for.
 */
class UntapRestrictionVisibilityTest : FunSpec({

    // Goblin Sharpshooter's shape: the restriction is printed on the creature itself.
    val printedDoesntUntap = card("Printed Sleeper") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        flags(AbilityFlag.DOESNT_UNTAP)
    }

    // Charmed Sleep / Tsabo's Web's shape: another permanent grants the restriction continuously.
    val grantsDoesntUntap = card("Freezing Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.AllCreatures)
        }
    }

    // Spider-Woman's shape: the stronger restriction, which blocks untap *effects* too.
    val grantsCantBecomeUntapped = card("Web Cocoon Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantKeyword(AbilityFlag.CANT_BECOME_UNTAPPED.name, GroupFilter.AllCreatures)
        }
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + printedDoesntUntap + grantsDoesntUntap + grantsCantBecomeUntapped + plainBear)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    test("a creature that untaps normally carries no untap-restriction flag") {
        val d = driver()
        val player = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(player, "Plain Bear")

        val card = transformer(d).transform(d.state, viewingPlayerId = player).cards[bear].shouldNotBeNull()
        card.abilityFlags shouldNotContain AbilityFlag.DOESNT_UNTAP
        card.abilityFlags shouldNotContain AbilityFlag.CANT_BECOME_UNTAPPED
        card.isExerted shouldBe false
    }

    test("a printed DOESNT_UNTAP reaches abilityFlags for both players") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val sleeper = d.putCreatureOnBattlefield(player, "Printed Sleeper")

        transformer(d).transform(d.state, viewingPlayerId = player)
            .cards[sleeper].shouldNotBeNull().abilityFlags shouldContain AbilityFlag.DOESNT_UNTAP
        transformer(d).transform(d.state, viewingPlayerId = opponent)
            .cards[sleeper].shouldNotBeNull().abilityFlags shouldContain AbilityFlag.DOESNT_UNTAP
    }

    test("a granted DOESNT_UNTAP reaches abilityFlags, and disappears with its source") {
        val d = driver()
        val player = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(player, "Plain Bear")
        val engine = d.putPermanentOnBattlefield(player, "Freezing Engine")

        transformer(d).transform(d.state, viewingPlayerId = player)
            .cards[bear].shouldNotBeNull().abilityFlags shouldContain AbilityFlag.DOESNT_UNTAP

        // The restriction is a continuous effect, so the badge has to vanish when the grant does —
        // a stale lock on a freed permanent is worse than no lock at all.
        d.moveToGraveyard(engine)
        transformer(d).transform(d.state, viewingPlayerId = player)
            .cards[bear].shouldNotBeNull().abilityFlags shouldNotContain AbilityFlag.DOESNT_UNTAP
    }

    test("a granted CANT_BECOME_UNTAPPED reaches abilityFlags on an opponent's creature") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val victim = d.putCreatureOnBattlefield(opponent, "Plain Bear")
        d.putPermanentOnBattlefield(player, "Web Cocoon Engine")

        // Both seats see it: the badge is how the victim's controller learns their creature is
        // stuck, and how the locking player confirms the lock landed.
        transformer(d).transform(d.state, viewingPlayerId = player)
            .cards[victim].shouldNotBeNull().abilityFlags shouldContain AbilityFlag.CANT_BECOME_UNTAPPED
        transformer(d).transform(d.state, viewingPlayerId = opponent)
            .cards[victim].shouldNotBeNull().abilityFlags shouldContain AbilityFlag.CANT_BECOME_UNTAPPED
    }

    test("a tapped, restricted permanent reports both isTapped and the flag") {
        val d = driver()
        val player = d.activePlayer!!
        val sleeper = d.putCreatureOnBattlefield(player, "Printed Sleeper")
        d.tapPermanent(sleeper)

        // The pair is what the frost overlay keys on — tapped *and* restricted means "stays this
        // way", which is a different board read from an ordinary tapped creature.
        val card = transformer(d).transform(d.state, viewingPlayerId = player).cards[sleeper].shouldNotBeNull()
        card.isTapped shouldBe true
        card.abilityFlags shouldContain AbilityFlag.DOESNT_UNTAP
    }
})
