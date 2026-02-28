package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val CallToTheGrave = card("Call to the Grave") {
    manaCost = "{4}{B}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, that player sacrifices a non-Zombie creature.\nAt the beginning of the end step, if no creatures are on the battlefield, sacrifice Call to the Grave."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = ForceSacrificeEffect(
            GameObjectFilter.Creature.notSubtype(Subtype("Zombie")),
            1,
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }

    triggeredAbility {
        trigger = Triggers.EachEndStep
        triggerCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(0)
        )
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a346b4a-ac8a-4f99-9ed7-dd41102e56ce.jpg?1562527023"
    }
}
