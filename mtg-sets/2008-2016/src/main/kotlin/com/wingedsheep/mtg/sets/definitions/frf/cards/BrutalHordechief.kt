package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brutal Hordechief
 * {3}{B}
 * Creature — Orc Warrior
 * 3/3
 * Whenever a creature you control attacks, defending player loses 1 life and you gain 1 life.
 * {3}{R/W}{R/W}: Creatures your opponents control block this turn if able, and you choose how
 * those creatures block.
 *
 * The drain's defending player is the one the triggering attacker attacks (ruling), so creatures
 * split across two opponents drain each of them separately. The activated ability is two
 * rule-modifying effects for the rest of the turn: [Effects.MustBlockGroup] over opponents'
 * creatures and [Effects.ChooseBlockersThisTurn] for the block declarations those opponents owe.
 * Both reach creatures that arrive or change hands later (ruling), and the blocks you pick must
 * still be legal for their controllers.
 */
val BrutalHordechief = card("Brutal Hordechief") {
    manaCost = "{3}{B}"
    colorIdentity = "BRW"
    typeLine = "Creature — Orc Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever a creature you control attacks, defending player loses 1 life and you gain 1 life.\n" +
        "{3}{R/W}{R/W}: Creatures your opponents control block this turn if able, and you choose how those " +
        "creatures block."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.DefendingPlayer)) then
            Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R/W}{R/W}")
        effect = Effects.MustBlockGroup(GroupFilter.AllCreaturesOpponentsControl) then
            Effects.ChooseBlockersThisTurn(Player.EachOpponent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "64"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab8ec769-99cd-4542-9b4d-61ee41b622bc.jpg?1783938699"
        ruling("2014-11-24", "All blocking declarations must still be legal.")
        ruling("2014-11-24", "If there's a cost associated with having a creature block and you choose for that creature to block, its controller can choose to pay that cost or not. If that player decides to not pay that cost, you must propose a new set of blocking creatures.")
        ruling("2014-11-24", "You'll choose how each creature controlled by an opponent blocks, even if that creature wasn't on the battlefield or wasn't controlled by an opponent as the activated ability resolved.")
        ruling("2014-11-24", "In a multiplayer game, if more than one player activates Brutal Hordechief's activated ability on the same turn, the controller of the last ability to resolve will choose how any creatures controlled by players who didn't resolve this ability will block.")
        ruling("2014-11-24", "The defending player affected by Brutal Hordechief's triggered ability is determined relative to the creature that attacked. For example, if Brutal Hordechief attacked one opponent and two other creatures you control attacked another opponent, the first opponent would lose 1 life and the second opponent would lose 2 life. You'd gain a total of 3 life.")
    }
}
