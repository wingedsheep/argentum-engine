package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Diligent Zookeeper — {3}{G}
 * Creature — Human Citizen Ally
 * 4/4
 * Each non-Human creature you control gets +1/+1 for each of its creature types,
 * to a maximum of 10.
 */
val DiligentZookeeper = card("Diligent Zookeeper") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen Ally"
    oracleText = "Each non-Human creature you control gets +1/+1 for each of its creature types, to a maximum of 10."
    power = 4
    toughness = 4

    staticAbility {
        // Each affected creature gets +N/+N where N = min(its subtype count, 10).
        // EntityReference.AffectedEntity resolves per-entity inside EffectApplicator,
        // so each creature is evaluated against its own type count.
        val bonus = DynamicAmount.Min(
            DynamicAmount.EntityProperty(
                entity = EntityReference.AffectedEntity,
                numericProperty = EntityNumericProperty.SubtypeCount
            ),
            DynamicAmount.Fixed(10)
        )
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.HUMAN).youControl()),
            powerBonus = bonus,
            toughnessBonus = bonus
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Maojin Lee"
        flavorText = "\"I wish I could get her a big, open prairie like she likes. I'd let her hop her way to happiness.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21f52564-9820-42dc-a08d-459d51afc397.jpg?1771532900"
    }
}
