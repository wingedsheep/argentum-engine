package com.wingedsheep.mtg.sets.definitions.tmc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ninja Pizza
 * {2}{G}
 * Enchantment
 * Foods you control have "{T}, Sacrifice this artifact: Add one mana of any color."
 * At the beginning of your second main phase, create a Food token.
 *
 * "Foods" names every Food artifact, not just Food tokens (ruling 2026-01-27), so the group is
 * the Food subtype over artifacts you control. "Sacrifice this artifact" inside the granted
 * quote is the grantee sacrificing itself.
 */
val NinjaPizza = card("Ninja Pizza") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Foods you control have \"{T}, Sacrifice this artifact: Add one mana of any color.\"\n" +
        "At the beginning of your second main phase, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf),
                effect = Effects.AddManaOfChoice(),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype("Food").youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Brian Yuen"
        flavorText = "The pizza that vanishes quickly without a trace."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74c24ff9-d3d2-4ffb-bf8b-dba803481a99.jpg?1783904164"
        ruling("2026-01-27", "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token.")
    }
}
