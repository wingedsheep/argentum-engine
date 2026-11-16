package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atarka's Command
 * {R}{G}
 * Instant
 *
 * Choose two —
 * • Your opponents can't gain life this turn.
 * • Atarka's Command deals 3 damage to each opponent.
 * • You may put a land card from your hand onto the battlefield.
 * • Creatures you control get +1/+1 and gain reach until end of turn.
 *
 * `modal(chooseCount = 2)` — two different modes, none of them targeted.
 *
 * - The life-gain lock tags each opponent for the rest of the turn ([Effects.LockLifeGain], the
 *   Roiling Vortex shape); life gained earlier in the turn is unaffected.
 * - The land drop is [Patterns.Hand.putFromHand]'s up-to-one selection, which models the "you may";
 *   putting a land onto the battlefield this way isn't playing a land.
 * - The anthem iterates the creatures you control as the spell resolves (CR 611.2c), so a creature
 *   that enters later in the turn doesn't get the bonus.
 */
val AtarkasCommand = card("Atarka's Command") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Your opponents can't gain life this turn.\n" +
        "• Atarka's Command deals 3 damage to each opponent.\n" +
        "• You may put a land card from your hand onto the battlefield.\n" +
        "• Creatures you control get +1/+1 and gain reach until end of turn."

    spell {
        modal(chooseCount = 2) {
            mode("Your opponents can't gain life this turn") {
                effect = Effects.LockLifeGain(EffectTarget.PlayerRef(Player.EachOpponent), Duration.EndOfTurn)
            }
            mode("Atarka's Command deals 3 damage to each opponent") {
                effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
            }
            mode("You may put a land card from your hand onto the battlefield") {
                effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
            }
            mode("Creatures you control get +1/+1 and gain reach until end of turn") {
                effect = Patterns.Group.pumpAndGrantToAll(
                    power = 1,
                    toughness = 1,
                    keyword = Keyword.REACH,
                    filter = GroupFilter(GameObjectFilter.Creature.youControl())
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/903d78c9-c5b3-45c3-a6d0-7e92b4196ae3.jpg?1783938574"
        ruling("2015-02-25", "The first mode won't affect life that was gained earlier in the turn.")
        ruling("2015-02-25", "If you choose the third mode and put a land card onto the battlefield, that doesn't count as playing a land. For example, you can both play a land and put a land onto the battlefield this way during your turn.")
    }
}
