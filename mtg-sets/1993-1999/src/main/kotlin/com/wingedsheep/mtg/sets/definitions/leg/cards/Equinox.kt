package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Equinox
 * {W}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Counter target spell if it would destroy a land you control."
 *
 * The ability can target any spell; whether it would destroy a land you control is checked as it
 * resolves (`CardPredicate.WouldDestroy`), counting only destruction the spell itself performs.
 */
val Equinox = card("Equinox") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Counter target spell if it would destroy a land you control.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.If(
                    Conditions.TargetMatchesFilter(GameObjectFilter.Any.wouldDestroy(GameObjectFilter.Land.youControl())),
                    Effects.CounterSpell(),
                    descriptionOverride = "Counter target spell if it would destroy a land you control"
                ),
                targetRequirements = listOf(TargetObject(filter = TargetFilter.SpellOnStack))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/840c6586-a7a9-4ae8-96be-a995a0693eb6.jpg?1783948086"
    }
}
