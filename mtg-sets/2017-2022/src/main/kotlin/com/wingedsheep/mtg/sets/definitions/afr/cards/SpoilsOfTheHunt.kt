package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spoils of the Hunt
 * {2}{G}
 * Instant
 * Target creature you control gets +1/+0 until end of turn for each mana from a Treasure that was
 * spent to cast this spell. Then that creature deals damage equal to its power to target creature
 * an opponent controls.
 *
 * The bonus counts the spell's recorded mana provenance ([DynamicAmounts.manaSpentFromSubtype]) —
 * controlling an unused Treasure adds nothing. The damage reads the creature's power after the pump,
 * as the bite is sequenced after it.
 */
val SpoilsOfTheHunt = card("Spoils of the Hunt") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 until end of turn for each mana from a Treasure " +
        "that was spent to cast this spell. Then that creature deals damage equal to its power to target " +
        "creature an opponent controls."

    spell {
        val yours = target(TargetFilter.Creature.youControl())
        val theirs = target(TargetFilter.Creature.opponentControls())
        effect = Effects.ModifyStats(
            DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE),
            DynamicAmounts.fixed(0),
            yours,
        ) then Effects.DealDamage(DynamicAmounts.powerOf(yours), theirs, damageSource = yours)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Justine Cruz"
        flavorText = "\"Every coin helps rebuild the town they destroyed.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10dfdfd4-8621-40ef-a60f-74e9c107bbaf.jpg?1783926456"
    }
}
