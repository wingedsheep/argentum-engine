package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Annie Joins Up
 * {1}{R}{G}{W}
 * Legendary Enchantment
 *
 * When Annie Joins Up enters, it deals 5 damage to target creature or planeswalker an
 * opponent controls.
 * If a triggered ability of a legendary creature you control triggers, that ability
 * triggers an additional time.
 *
 * The second clause is the generic [AdditionalSourceTriggers] doubler (CR 603.2d),
 * scoped to legendary creatures you control. Annie herself is an enchantment, so the
 * "another" exclusion is moot, but excludeSelf stays true to match the filter's
 * "you control" intent without ever matching the source.
 */
val AnnieJoinsUp = card("Annie Joins Up") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Enchantment"
    oracleText = "When Annie Joins Up enters, it deals 5 damage to target creature or planeswalker an opponent controls.\n" +
        "If a triggered ability of a legendary creature you control triggers, that ability triggers an additional time."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target creature or planeswalker an opponent controls",
            TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        )
        effect = Effects.DealDamage(5, t)
    }

    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = GameObjectFilter.Creature.legendary().youControl(),
            excludeSelf = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Wylie Beckert"
        flavorText = "One last job, then she could retire in peace."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1624a5f4-f5bc-47c9-85de-c5520ee234ce.jpg?1712356038"
    }
}
