package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chillbringer — Ravnica Allegiance #33
 * {4}{U} · Creature — Elemental · 3 / 3
 */
val Chillbringer = card("Chillbringer") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, tap target creature an opponent controls. " +
        "It doesn't untap during its controller's next untap step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(t) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, t, Duration.UntilAfterAffectedControllersNextUntap())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Eytan Zana"
        flavorText = "If you can see your breath, it's too late to run."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/158547fa-7313-4e77-949f-afc68ebfb022.jpg?1783933712"
    }
}
