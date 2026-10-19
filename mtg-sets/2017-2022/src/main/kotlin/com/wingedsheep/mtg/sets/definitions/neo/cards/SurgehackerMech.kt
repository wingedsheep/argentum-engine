package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Surgehacker Mech — Kamigawa: Neon Dynasty #260 (canonical printing)
 * {4} · Artifact — Vehicle · 5/5
 *
 * Menace
 * When this Vehicle enters, it deals damage equal to twice the number of Vehicles you control
 * to target creature or planeswalker an opponent controls.
 * Crew 4
 *
 * The Vehicle count is read on resolution and includes the Mech itself while it is still on the
 * battlefield.
 */
val SurgehackerMech = card("Surgehacker Mech") {
    manaCost = "{4}"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Menace\nWhen this Vehicle enters, it deals damage equal to twice the number of " +
        "Vehicles you control to target creature or planeswalker an opponent controls.\nCrew 4 " +
        "(Tap any number of creatures you control with total power 4 or more: This Vehicle becomes " +
        "an artifact creature until end of turn.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE)
            ).count() * 2,
            t
        )
        description = "When this Vehicle enters, it deals damage equal to twice the number of " +
            "Vehicles you control to target creature or planeswalker an opponent controls."
    }

    keywordAbility(KeywordAbility.crew(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c4bf679-01cc-4786-a3bd-89c8ae70fdfa.jpg?1783923820"
    }
}
