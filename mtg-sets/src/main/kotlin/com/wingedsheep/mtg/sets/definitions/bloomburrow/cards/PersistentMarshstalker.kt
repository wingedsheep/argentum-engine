package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Persistent Marshstalker
 * {1}{B}
 * Creature — Rat Berserker
 * 3/1
 * This creature gets +1/+0 for each other Rat you control.
 * Threshold — Whenever you attack with one or more Rats, if there are seven or more cards
 * in your graveyard, you may pay {2}{B}. If you do, return this card from your graveyard
 * to the battlefield tapped and attacking.
 */
val PersistentMarshstalker = card("Persistent Marshstalker") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Rat Berserker"
    power = 3
    toughness = 1
    oracleText = "This creature gets +1/+0 for each other Rat you control.\nThreshold — Whenever you attack with one or more Rats, if there are seven or more cards in your graveyard, you may pay {2}{B}. If you do, return this card from your graveyard to the battlefield tapped and attacking."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            target = StaticTarget.SourceCreature,
            powerBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype("Rat"),
                excludeSelf = true
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Creature.withSubtype("Rat"))
        triggerZone = Zone.GRAVEYARD
        triggerCondition = Conditions.CardsInGraveyardAtLeast(7)
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{B}"),
            effect = MoveToZoneEffect(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD,
                placement = ZonePlacement.TappedAndAttacking
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b900c71-713b-4b7e-b4be-ad9f4aa0c139.jpg?1721426469"
    }
}
