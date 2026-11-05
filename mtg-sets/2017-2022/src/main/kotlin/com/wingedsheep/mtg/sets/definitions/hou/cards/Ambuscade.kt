package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ambuscade — Hour of Devastation #110
 * {2}{G}
 * Instant
 * Target creature you control gets +1/+0 until end of turn. It deals damage equal to its power to
 * target creature an opponent controls.
 *
 * The Clear Shot rail: pump the first target, then have it deal damage equal to its (already
 * buffed) power to the second. `damageSource = t1` makes the pumped creature the source of the
 * damage, so lifelink, deathtouch and "dealt damage by" riders see it.
 */
val Ambuscade = card("Ambuscade") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 until end of turn. It deals damage equal to its " +
        "power to target creature an opponent controls."

    spell {
        val t1 = target(TargetFilter.Creature.youControl())
        val t2 = target(TargetFilter.Creature.opponentControls())
        effect = Effects.ModifyStats(1, 0, t1) then
            Effects.DealDamage(DynamicAmounts.powerOf(t1), t2, damageSource = t1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Kev Walker"
        flavorText = "\"Spent a lifetime learning from predators. Picked up a few tricks.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfc06303-cb35-4feb-b23d-7ac0a2fa7ce3.jpg?1783936021"
        ruling(
            "2017-07-14",
            "If the creature you control leaves the battlefield before Ambuscade resolves, Ambuscade has no " +
                "effect and no damage is dealt. If the creature an opponent controls leaves the battlefield " +
                "instead, the creature you control gets +1/+0 even though it won't deal any damage."
        )
    }
}
