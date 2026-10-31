package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers
import com.wingedsheep.sdk.scripting.ZoneChangeTriggerCause

/**
 * Hushbringer
 * {1}{W}
 * Creature — Faerie
 * 1/2
 *
 * Flying, lifelink
 * Creatures entering or dying don't cause abilities to trigger.
 *
 * Torpor Orb's [SuppressZoneChangeTriggers] lock widened to deaths. The dying half looks back in
 * time, so creatures dying alongside Hushbringer (or Hushbringer itself) still trigger nothing.
 */
val Hushbringer = card("Hushbringer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Faerie"
    oracleText = "Flying, lifelink\nCreatures entering or dying don't cause abilities to trigger."
    power = 1
    toughness = 2
    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = SuppressZoneChangeTriggers(
            causes = setOf(ZoneChangeTriggerCause.ENTERING, ZoneChangeTriggerCause.DYING)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Bastien L. Deharme"
        flavorText = "\"Let's begin this conversation again, and this time you will only listen.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/6/663b3e6f-1099-4de8-a0a7-6f1919c38010.jpg?1783932672"
    }
}
