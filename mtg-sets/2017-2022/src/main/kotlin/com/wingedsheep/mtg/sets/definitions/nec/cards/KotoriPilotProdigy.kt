package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kotori, Pilot Prodigy — Neon Dynasty Commander #2 (canonical printing)
 * {1}{W}{U} · Legendary Creature — Moonfolk Pilot · 2/4
 *
 * Vehicles you control have crew 2.
 * At the beginning of combat on your turn, target artifact creature you control gains lifelink
 * and vigilance until end of turn.
 *
 * The crew grant is the Astor, Bearer of Blades shape — `GrantKeyword(grantedCrew(2), vehicles)` —
 * an additional crew ability alongside each Vehicle's own printed one.
 */
val KotoriPilotProdigy = card("Kotori, Pilot Prodigy") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Moonfolk Pilot"
    power = 2
    toughness = 4
    oracleText = "Vehicles you control have crew 2.\n" +
        "At the beginning of combat on your turn, target artifact creature you control gains " +
        "lifelink and vigilance until end of turn."

    staticAbility {
        ability = GrantKeyword(
            KeywordAbility.grantedCrew(2),
            GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE).youControl()),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter(GameObjectFilter.ArtifactCreature.youControl()))
        effect = Effects.GrantKeyword(Keyword.LIFELINK, t) then
            Effects.GrantKeyword(Keyword.VIGILANCE, t)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Aaron J. Riley"
        flavorText = "\"Some people test limits. I prefer to demolish them.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fea6c1c8-eff9-4b66-b96d-e23016f0bb62.jpg?1783924000"
    }
}
