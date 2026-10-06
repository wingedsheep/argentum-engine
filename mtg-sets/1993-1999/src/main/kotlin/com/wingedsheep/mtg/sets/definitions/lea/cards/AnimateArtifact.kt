package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Animate Artifact — Limited Edition Alpha #48
 * {3}{U} · Enchantment — Aura
 *
 * March of the Machines narrowed to the enchanted artifact: one multi-layer static (CR 613.6)
 * over `Artifact.notCreature().attachedToBySource()` — Layer 4 adds CREATURE, Layer 7b sets P/T
 * to the artifact's own mana value. The "isn't a creature" filter is not creature-keyed, so the
 * set resolved at collection stays locked through Layer 7b instead of dropping the artifact the
 * moment Layer 4 animates it. An artifact that is already a creature is left alone entirely.
 */
val AnimateArtifact = card("Animate Artifact") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "As long as enchanted artifact isn't a creature, it's an artifact creature with power and " +
        "toughness each equal to its mana value."
    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    val enchantedNoncreatureArtifact = GroupFilter(GameObjectFilter.Artifact.notCreature().attachedToBySource())
    val manaValue: DynamicAmount = DynamicAmounts.manaValueOf(EffectTarget.AffectedEntity)

    staticAbility {
        ability = CompositeStaticAbility(
            listOf(
                GrantCardType(cardType = "CREATURE", filter = enchantedNoncreatureArtifact),
                SetBasePowerToughnessDynamicStatic(
                    power = manaValue,
                    toughness = manaValue,
                    filter = enchantedNoncreatureArtifact
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/664b46f5-0424-4f4e-9f26-6bd2cf5e0357.jpg?1783948707"
    }
}
