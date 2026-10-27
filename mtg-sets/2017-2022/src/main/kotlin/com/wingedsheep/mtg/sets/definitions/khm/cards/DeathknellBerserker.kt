package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Deathknell Berserker — Kaldheim #83
 * {1}{B} · Creature — Elf Berserker · 2/2
 *
 * When this creature dies, if its power was 3 or greater, create a 2/2 black Zombie Berserker
 * creature token.
 *
 * "Its power was" is read as the Berserker last existed on the battlefield (CR 603.10, 608.2h):
 * the intervening-if compares the triggering entity's last-known power, so a +1/+1 counter or a
 * pump it died with counts.
 */
val DeathknellBerserker = card("Deathknell Berserker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Berserker"
    power = 2
    toughness = 2
    oracleText = "When this creature dies, if its power was 3 or greater, create a 2/2 black Zombie " +
        "Berserker creature token."

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.Power),
            ComparisonOperator.GTE,
            DynamicAmounts.fixed(3),
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Berserker")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Zoltan Boros"
        flavorText = "Sven felled the elf with an arrow through the chest, but moments later, the corpse " +
            "rose with a sneer to fight again.\n—Longbeard Saga"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9f2029f-ffda-4374-9a78-79866ac23fca.jpg?1783928252"
    }
}
