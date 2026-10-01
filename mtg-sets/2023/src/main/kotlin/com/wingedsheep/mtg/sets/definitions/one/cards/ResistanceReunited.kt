package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Resistance Reunited
 * {1}{W}
 * Instant
 * Target creature gets +2/+2 until end of turn.
 * Equipped creatures you control gain indestructible until end of turn.
 *
 * The set of equipped creatures is locked in on resolution (CR 611.2c): `ForEachInGroup` snapshots
 * the group, so a creature equipped later this turn doesn't gain indestructible.
 */
val ResistanceReunited = card("Resistance Reunited") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn.\n" +
        "Equipped creatures you control gain indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy them.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl().equipped()),
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Aurore Folny"
        flavorText = "\"Well, aren't you a sight for sore eyes!\" said Koth. \"I was about to say the same, my friend,\" Elspeth replied."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbc4b5ca-557e-4465-9725-9e7a15590258.jpg?1783918074"
    }
}
