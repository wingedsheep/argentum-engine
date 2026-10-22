package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Candletrap (Innistrad: Midnight Hunt #9)
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has defender.
 * Prevent all combat damage that would be dealt by enchanted creature.
 * Coven — {2}{W}, Sacrifice this Aura: Exile enchanted creature. Activate only if you control three
 * or more creatures with different powers.
 *
 * Sacrificing the Aura is a cost; the sacrifice cost keeps the Aura's attachment on its graveyard
 * object, so "enchanted creature" still names the creature it was attached to on resolution.
 */
val Candletrap = card("Candletrap") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has defender.\n" +
        "Prevent all combat damage that would be dealt by enchanted creature.\n" +
        "Coven — {2}{W}, Sacrifice this Aura: Exile enchanted creature. Activate only if you control " +
        "three or more creatures with different powers."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER)
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.attachedToBySource(),
                damageType = DamageType.Combat,
            ),
        ),
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.SacrificeSelf)
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    left = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature,
                    ).distinctValues(CardNumericProperty.POWER),
                    operator = ComparisonOperator.GTE,
                    right = 3,
                ),
            ),
        )
        description = "Coven — {2}{W}, Sacrifice this Aura: Exile enchanted creature. Activate only if " +
            "you control three or more creatures with different powers."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32869a36-f21e-4348-a787-871daa6151df.jpg?1783925665"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}
