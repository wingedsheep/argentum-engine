package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Akul the Unrepentant
 * {B}{B}{R}{R}
 * Legendary Creature — Scorpion Dragon Rogue
 * 5/5
 *
 * Flying, trample
 * Sacrifice three other creatures: You may put a creature card from your hand onto the
 * battlefield. Activate only as a sorcery and only once each turn.
 *
 * The cost "Sacrifice three other creatures" is a single [CostAtom.Sacrifice] with `count = 3`
 * and `excludeSelf = true`, so Akul itself can never be one of the three sacrifices (and the
 * engine enforces that three other creatures actually exist before the ability can be activated).
 *
 * "You may put a creature card from your hand onto the battlefield" is a gather → choose-up-to-1
 * → move pipeline: gathering creature cards from hand, letting the controller pick up to one
 * (choosing zero = declining, which is how the "may" is expressed), then moving the chosen card
 * to the battlefield under its controller's control.
 *
 * "Activate only as a sorcery" maps to [TimingRule.SorcerySpeed]; "only once each turn" maps to
 * [ActivationRestriction.OncePerTurn]. These are orthogonal — sorcery speed is the timing window,
 * once-per-turn is the activation cap.
 */
val AkulTheUnrepentant = card("Akul the Unrepentant") {
    manaCost = "{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Scorpion Dragon Rogue"
    power = 5
    toughness = 5
    oracleText = "Flying, trample\n" +
        "Sacrifice three other creatures: You may put a creature card from your hand onto the " +
        "battlefield. Activate only as a sorcery and only once each turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    activatedAbility {
        cost = AbilityCost.Atom(
            CostAtom.Sacrifice(GameObjectFilter.Creature, count = 3, excludeSelf = true)
        )
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                )
            )
            val chosen = chooseUpTo(
                1,
                from = candidates,
                useTargetingUI = false,
                prompt = "You may put a creature card from your hand onto the battlefield"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68fd8548-50db-4243-9154-377f32408d58.jpg?1712356029"
    }
}
