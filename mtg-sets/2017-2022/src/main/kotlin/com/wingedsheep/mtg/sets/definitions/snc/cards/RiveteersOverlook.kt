package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Riveteers Overlook — Streets of New Capenna #255. One of SNC's family fetch lands.
 *
 * When this land enters, sacrifice it. When you do, search your library for a basic Swamp,
 * Mountain, or Forest card, put it onto the battlefield tapped, then shuffle and you gain 1 life.
 *
 * The sacrifice is gated on the land still being on the battlefield: if it left before the
 * enters trigger resolved, it can't be sacrificed, so the reflexive "when you do" never triggers.
 */
val RiveteersOverlook = card("Riveteers Overlook") {
    typeLine = "Land"
    oracleText = "When this land enters, sacrifice it. When you do, search your library for a basic " +
        "Swamp, Mountain, or Forest card, put it onto the battlefield tapped, then shuffle and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.SourceInZone(Zone.BATTLEFIELD),
            then = Effects.ReflexiveTrigger(
                action = Effects.SacrificeTarget(EffectTarget.Self),
                optional = false,
                reflexiveEffect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                        listOf(Subtype.SWAMP, Subtype.MOUNTAIN, Subtype.FOREST)
                    ),
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                ) then Effects.GainLife(1),
                descriptionOverride = "Sacrifice this land. When you do, search your library for a basic " +
                    "Swamp, Mountain, or Forest card, put it onto the battlefield tapped, then shuffle and you gain 1 life."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Lucas Staniec"
        flavorText = "Along the outer girders, in gold filigree, are inscribed the names of all those who fell to their deaths during construction."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d161f81-c01a-4c91-b025-52dcb1881638.jpg?1783923054"
    }
}
