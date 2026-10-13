package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leyline Binding — domain is a self-cast generic reduction over [DynamicAmounts.domain] (the
 * caster's basic land types, read from projected subtypes); the exile is the same
 * [Effects.MoveUntilSourceLeaves] Citizen's Arrest uses, so nothing is exiled if Leyline Binding
 * is already gone when the trigger resolves.
 */
val LeylineBinding = card("Leyline Binding") {
    manaCost = "{5}{W}"
    typeLine = "Enchantment"
    oracleText = "Flash\nDomain — This spell costs {1} less to cast for each basic land type among lands you control.\nWhen this enchantment enters, exile target nonland permanent an opponent controls until this enchantment leaves the battlefield."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.domain())
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter(GameObjectFilter.NonlandPermanent.opponentControls()))
        effect = Effects.MoveUntilSourceLeaves(victim, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c3ac3dd-35db-447f-8674-37b4680a1ef7.jpg?1783921363"
        ruling("2024-04-12", "Leyline Binding's domain ability doesn't change its mana value, which is always 6.")
        ruling("2022-09-09", "If Leyline Binding leaves the battlefield before its enters-the-battlefield ability resolves, the target permanent won't be exiled.")
        ruling("2022-09-09", "If a token is exiled, it ceases to exist. It won't be returned to the battlefield.")
    }
}
