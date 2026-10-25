package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Roil Elemental
 * {3}{U}{U}{U}
 * Creature — Elemental
 * 3/2
 * Flying
 * Landfall — Whenever a land you control enters, you may gain control of target creature for as
 * long as you control this creature.
 *
 * The steal is [Effects.GainControl] for [Duration.WhileYouControlSource] — the same gate Olivia
 * Voldaren uses. It is gated on the source being on the battlefield under the effect's controller,
 * so if Roil Elemental leaves or changes controller before the trigger resolves, the control
 * change never takes hold (2009 ruling), and it ends for good once you stop controlling it.
 */
val RoilElemental = card("Roil Elemental") {
    manaCost = "{3}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "Flying\n" +
        "Landfall — Whenever a land you control enters, you may gain control of target creature for as long as you control this creature."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        optional = true
        effect = Effects.GainControl(creature, Duration.WhileYouControlSource("this creature"))
        description = "Landfall — Whenever a land you control enters, you may gain control of target creature for as long as you control this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Raymond Swanland"
        flavorText = "A vortex that devours everything—even the souls of the living."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66139c73-8e14-433d-bcb6-ae3e518bcdfb.jpg?1783942161"
        ruling("2009-10-01", "If Roil Elemental leaves the battlefield, you no longer control it, so all of its control-change effects end.")
        ruling("2009-10-01", "If Roil Elemental ceases to be under your control before its ability resolves, you won't gain control of the targeted creature at all.")
        ruling("2009-10-01", "You may target a creature you already control with Roil Elemental's ability. This will usually have no visible effect, but it will overwrite any previous control-change effects.")
    }
}
