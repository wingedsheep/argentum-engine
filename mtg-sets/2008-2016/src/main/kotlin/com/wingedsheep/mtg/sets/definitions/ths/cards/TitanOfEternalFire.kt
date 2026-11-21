package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Titan of Eternal Fire
 * {5}{R}
 * Creature — Giant
 * 5/6
 * Each Human creature you control has "{R}, {T}: This creature deals 1 damage to any target."
 */
val TitanOfEternalFire = card("Titan of Eternal Fire") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 5
    toughness = 6
    oracleText = "Each Human creature you control has \"{R}, {T}: This creature deals 1 damage to any target.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
                val t = target(Targets.Any)
                effect = Effects.DealDamage(1, t, damageSource = EffectTarget.Self)
                description = "{R}, {T}: This creature deals 1 damage to any target."
            },
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Aleksi Briclot"
        flavorText = "There is no gift more precious or more perilous than fire."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeb8e4df-930c-4fb1-9258-184ff6c415d0.jpg?1783939752"
    }
}
