package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Emeria, the Sky Ruin — Zendikar #213
 * Land
 * This land enters tapped.
 * At the beginning of your upkeep, if you control seven or more Plains, you may return target
 * creature card from your graveyard to the battlefield.
 * {T}: Add {W}.
 *
 * The Plains count is an intervening-if (CR 603.4): checked as the upkeep begins and again on
 * resolution (ruling). "Plains" is the land subtype, so nonbasic Plains count too.
 */
val EmeriaTheSkyRuin = card("Emeria, the Sky Ruin") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "At the beginning of your upkeep, if you control seven or more Plains, you may return target " +
        "creature card from your graveyard to the battlefield.\n" +
        "{T}: Add {W}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(7, GameObjectFilter.Land.withSubtype("Plains"))
        optional = true
        val creature = target(TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdf6016e-71e7-4339-80f9-78e9aa44c54b.jpg?1783942124"
        ruling("2009-10-01", "Emeria, the Sky Ruin's triggered ability has an \"intervening 'if' clause.\" That means (1) the ability won't trigger at all unless you control seven or more Plains as your upkeep starts, and (2) the ability will do nothing if you control fewer than seven Plains by the time it resolves.")
    }
}
