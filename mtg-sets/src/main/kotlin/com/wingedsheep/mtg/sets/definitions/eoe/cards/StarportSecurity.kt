package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Starport Security
 * {W}
 * Artifact Creature — Robot Soldier
 * 1/1
 * {3}{W}, {T}: Tap another target creature. This ability costs {2} less to
 * activate if you control a creature with a +1/+1 counter on it.
 */
val StarportSecurity = card("Starport Security") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot Soldier"
    power = 1
    toughness = 1
    oracleText = "{3}{W}, {T}: Tap another target creature. This ability costs {2} less to activate if you control a creature with a +1/+1 counter on it."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}"), Costs.Tap)
        val creature = target(
            "another target creature",
            TargetCreature(filter = TargetFilter.OtherCreature)
        )
        effect = Effects.Tap(creature)
        genericCostReduction = DynamicAmount.Conditional(
            condition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE)
            ),
            ifTrue = DynamicAmount.Fixed(2),
            ifFalse = DynamicAmount.Fixed(0)
        )
        description = "{3}{W}, {T}: Tap another target creature. This ability costs {2} less to activate if you control a creature with a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Lie Setiawan"
        flavorText = "\"You are a suspect in an ongoing investigation. Do not resist apprehension.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/238cb1db-6c41-4fe1-bc34-340048dfde18.jpg?1752946704"
    }
}
