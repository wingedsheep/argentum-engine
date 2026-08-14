package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hylda's Crown of Winter
 * {3}
 * Legendary Artifact
 *
 * {1}, {T}: Tap target creature. This ability costs {1} less to activate during your turn.
 * {3}, Sacrifice Hylda's Crown of Winter: Draw a card for each tapped creature your opponents
 * control.
 *
 * The tap ability's discount is a plain `genericCostReduction` gated on
 * [Conditions.IsYourTurn] (the Starport Security shape with a turn condition instead of a board
 * condition), so on your own turn it is a free `{T}` tapper and on an opponent's turn it costs
 * {1}. The reduction rail only eats generic mana, which is all this cost is.
 *
 * The sacrifice ability counts *tapped* creatures with
 * [DynamicAmount.AggregateBattlefield]`(Player.EachOpponent, Creature.tapped())` — the `player`
 * scope does the "your opponents control" half, and the count is read at resolution, so creatures
 * this Crown tapped earlier in the turn (and creatures that attacked) are all included.
 */
val HyldasCrownOfWinter = card("Hylda's Crown of Winter") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "{1}, {T}: Tap target creature. This ability costs {1} less to activate during " +
        "your turn.\n" +
        "{3}, Sacrifice Hylda's Crown of Winter: Draw a card for each tapped creature your " +
        "opponents control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target("target creature", TargetCreature())
        effect = Effects.Tap(creature)
        genericCostReduction = DynamicAmount.Conditional(
            condition = Conditions.IsYourTurn,
            ifTrue = DynamicAmount.Fixed(1),
            ifFalse = DynamicAmount.Fixed(0),
        )
        description = "{1}, {T}: Tap target creature. This ability costs {1} less to activate " +
            "during your turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(
            DynamicAmount.AggregateBattlefield(
                player = Player.EachOpponent,
                filter = GameObjectFilter.Creature.tapped(),
            )
        )
        description = "{3}, Sacrifice Hylda's Crown of Winter: Draw a card for each tapped " +
            "creature your opponents control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Volkan Baǵa"
        flavorText = "With each day she wore the crown, her icy kingdom spread, and her heart " +
            "grew ever colder."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0d4a6c0-f00e-45a7-9c88-899460007020.jpg?1783915057"
    }
}
