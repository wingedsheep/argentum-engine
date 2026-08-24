package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LandsCantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.PlayersCantPlayLands
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Worms of the Earth
 * {2}{B}{B}{B}
 * Enchantment
 * Players can't play lands.
 * Lands can't enter the battlefield.
 * At the beginning of each upkeep, any player may sacrifice two lands of their choice or have this
 * enchantment deal 5 damage to that player. If a player does either, destroy this enchantment.
 *
 * The two lock lines are two statics because they are two different events, and neither subsumes
 * the other: [PlayersCantPlayLands] stops the *special action* of playing a land, while
 * [LandsCantEnterTheBattlefield] also catches a land arriving by an effect. A card printing only
 * the second would still let lands be played from hand; one printing only the first would lose to
 * any fetch effect. Worms prints both, so the engine grew both.
 *
 * The escape clause is a per-player offer, not a per-controller one — "*any* player may" — so it is
 * a `ForEachEffect` over players in APNAP order, each wrapped in a `MayDecide` gate of its own.
 * Inside, the two ways out are a modal: sacrifice two lands, or take 5 damage. Either destroys the
 * enchantment, which is the point — the damage branch is how a player with no lands left still buys
 * their way out.
 *
 * Iterating in APNAP order matters here: once one player pays, Worms is gone, and later players are
 * offered nothing.
 */
val WormsOfTheEarth = card("Worms of the Earth") {
    manaCost = "{2}{B}{B}{B}"
    typeLine = "Enchantment"
    oracleText = "Players can't play lands.\nLands can't enter the battlefield.\nAt the beginning " +
        "of each upkeep, any player may sacrifice two lands of their choice or have this " +
        "enchantment deal 5 damage to that player. If a player does either, destroy this enchantment."

    staticAbility { ability = PlayersCantPlayLands(Player.Each) }
    staticAbility { ability = LandsCantEnterTheBattlefield }

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = ForEachEffect(
            space = IterationSpace.Players(Player.ActivePlayerFirst),
            body = GatedEffect(
                gate = Gate.MayDecide(
                    prompt = "Buy your way out of Worms of the Earth?",
                ),
                then = ModalEffect(
                    modes = listOf(
                        Mode(
                            description = "Sacrifice two lands",
                            // Gated on actually having two lands, because nothing else stops this
                            // mode being chosen. `ModalEffect` never feasibility-filters its modes,
                            // and `Sacrifice` below the required count is a silent no-op that
                            // returns *success* — so a player with one land or none could pick this
                            // branch, sacrifice nothing, and still reach the `Destroy` below,
                            // walking out of the lock for free. That player is exactly who the lock
                            // is working on, so it was the common case, not a corner one. Printed,
                            // their only way out is the 5-damage mode.
                            effect = GatedEffect(
                                gate = Gate.WhenCondition(Conditions.ControlLandsAtLeast(2)),
                                then = Effects.Composite(
                                    Effects.Sacrifice(
                                        GameObjectFilter.Land,
                                        count = 2,
                                        target = EffectTarget.Controller,
                                    ),
                                    Effects.Destroy(EffectTarget.Self),
                                ),
                            ),
                        ),
                        Mode(
                            description = "Take 5 damage from Worms of the Earth",
                            effect = Effects.Composite(
                                Effects.DealDamage(5, EffectTarget.Controller),
                                Effects.Destroy(EffectTarget.Self),
                            ),
                        ),
                    ),
                    countsAsModalSpell = false,
                ),
                decisionMaker = EffectTarget.Controller,
            ),
        )
        description = "At the beginning of each upkeep, any player may sacrifice two lands of " +
            "their choice or have this enchantment deal 5 damage to that player. If a player does " +
            "either, destroy this enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Anson Maddocks"
        flavorText = "The ground collapsed, leaving nothing but the great Worms' mucous residues."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65a97821-ca5b-46fb-af08-86de81d0daac.jpg?1783947937"
    }
}
