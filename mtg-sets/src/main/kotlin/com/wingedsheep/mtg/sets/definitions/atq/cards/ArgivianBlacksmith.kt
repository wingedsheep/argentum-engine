package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Argivian Blacksmith
 * {1}{W}{W}
 * Creature — Human Artificer
 * 2/2
 * {T}: Prevent the next 2 damage that would be dealt to target artifact creature this turn.
 */
val ArgivianBlacksmith = card("Argivian Blacksmith") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "{T}: Prevent the next 2 damage that would be dealt to target artifact creature this turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target(
            "target artifact creature",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.ArtifactCreature))
        )
        effect = Effects.PreventNextDamage(2, EffectTarget.ContextTarget(0))
        description = "{T}: Prevent the next 2 damage that would be dealt to target artifact creature this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Kerstin Kaman"
        flavorText = "Through years of study and training, the Blacksmiths of Argive became adept at reassembling the mangled remains of the strange, mechanical creatures abounding in their native land."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f604338-5ee4-4c47-ad5a-5c805c96c8de.jpg?1562914930"
    }
}
