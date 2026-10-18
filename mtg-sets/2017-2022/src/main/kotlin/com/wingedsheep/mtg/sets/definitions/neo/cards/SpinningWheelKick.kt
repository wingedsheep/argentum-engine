package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spinning Wheel Kick — Kamigawa: Neon Dynasty #207 (canonical printing)
 * {X}{X}{G}{G} · Sorcery
 *
 * Target creature you control deals damage equal to its power to each of X target creatures
 * and/or planeswalkers.
 *
 * The X targets are visited with `ForEachTargetOf`, so the creature you control (a separate
 * declaration) is the damage source and is never hit unless it was also chosen among the X. Its
 * power is read once, before any damage, so every victim takes the same amount even if the kicker
 * hits itself first with wither or infect.
 * If it is an illegal target on resolution no damage is dealt; an illegal one among the X is
 * skipped (CR 608.2b).
 */
val SpinningWheelKick = card("Spinning Wheel Kick") {
    manaCost = "{X}{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to each of X target " +
        "creatures and/or planeswalkers."

    spell {
        val kicker = target(TargetFilter.CreatureYouControl)
        val victims = targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
            exactly = DynamicAmounts.xValue()
        )
        effect = Effects.StoreNumber("kickPower", DynamicAmounts.powerOf(kicker)) then
            Effects.ForEachTargetOf(
                victims,
                Effects.DealDamage(
                    amount = DynamicAmounts.storedNumber("kickPower"),
                    target = EffectTarget.IterationEntity,
                    damageSource = kicker
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Evyn Fong"
        flavorText = "Members of the Order of Jukai commune with kami to enhance their martial prowess."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f36ddd7-82c1-45ef-a966-ae2a34c540e1.jpg?1783923839"
    }
}
