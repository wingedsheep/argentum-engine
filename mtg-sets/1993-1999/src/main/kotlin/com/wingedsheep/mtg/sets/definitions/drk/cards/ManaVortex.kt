package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Vortex
 * {1}{U}{U}
 * Enchantment
 * When you cast this spell, counter it unless you sacrifice a land.
 * At the beginning of each player's upkeep, that player sacrifices a land of their choice.
 * When there are no lands on the battlefield, sacrifice this enchantment.
 *
 * Three clauses, each a different flavour of trigger. The entry tax is a *cast* trigger that
 * counters its own spell — the ability resolves before the spell does, so a Vortex whose controller
 * declines never enters at all.
 *
 * The upkeep clause is where "that player" matters: with `Triggers.EachUpkeep` the ability's own
 * controller stays the Vortex's controller, so the sacrifice is aimed at `Player.TriggeringPlayer`,
 * which for a step trigger is the player whose upkeep it is. Aiming it at the controller instead
 * would make the Vortex eat only its owner's lands.
 *
 * The last clause is Drop of Honey's shape — a state-triggered ability (CR 603.8) that fires the
 * moment the board empties of lands, including as the Vortex's own upkeep trigger takes the last
 * one. So the card reliably ends itself rather than sitting on an empty board.
 */
val ManaVortex = card("Mana Vortex") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When you cast this spell, counter it unless you sacrifice a land.\n" +
        "At the beginning of each player's upkeep, that player sacrifices a land of their choice.\n" +
        "When there are no lands on the battlefield, sacrifice this enchantment."

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = PayOrSufferEffect(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Land),
            suffer = Effects.CounterTriggeringSpell(),
        )
        description = "When you cast this spell, counter it unless you sacrifice a land."
    }

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.Sacrifice(
            GameObjectFilter.Land,
            count = 1,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of each player's upkeep, that player sacrifices a land of their choice."
    }

    stateTriggeredAbility {
        condition = Conditions.NoLandsOnBattlefield
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When there are no lands on the battlefield, sacrifice this enchantment"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f857a00a-82e0-4227-86ee-1f9c7ca232ae.jpg?1783947942"
    }
}
