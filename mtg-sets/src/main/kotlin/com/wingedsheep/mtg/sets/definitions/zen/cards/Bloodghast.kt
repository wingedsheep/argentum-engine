package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Bloodghast — Zendikar #83. Canonical definition for later reprints. */
val Bloodghast = card("Bloodghast") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Spirit"
    power = 2
    toughness = 1
    oracleText = "This creature can't block.\n" +
        "This creature has haste as long as an opponent has 10 or less life.\n" +
        "Landfall — Whenever a land you control enters, you may return this card from your graveyard to the battlefield."

    staticAbility { ability = CantBlock() }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = Conditions.AnOpponentLifeAtMost(10),
        )
    }
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Land.youControl(),
            binding = TriggerBinding.ANY,
        )
        triggerZone = Zone.GRAVEYARD
        effect = MayEffect(
            Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
        )
        description = "Landfall — Whenever a land you control enters, you may return this card from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63870c81-63bf-4a9a-aeb5-74c6eaded9f1.jpg?1783942155"
        ruling(
            "2009-10-01",
            "Bloodghast's landfall ability triggers only if it's already in your graveyard at the time a land enters under your control.",
        )
    }
}
