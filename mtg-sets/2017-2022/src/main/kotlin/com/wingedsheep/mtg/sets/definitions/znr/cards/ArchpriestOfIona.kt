package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Archpriest of Iona — Zendikar Rising #5
 * {W} · Creature — Human Cleric · * /2
 *
 * Archpriest of Iona's power is equal to the number of creatures in your party.
 * At the beginning of combat on your turn, if you have a full party, target creature gets +1/+1
 * and gains flying until end of turn.
 */
val ArchpriestOfIona = card("Archpriest of Iona") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Archpriest of Iona's power is equal to the number of creatures in your party. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "At the beginning of combat on your turn, if you have a full party, target creature gets +1/+1 " +
        "and gains flying until end of turn."
    dynamicPower(DynamicAmounts.partySize())
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.FullParty()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature) then
            Effects.GrantKeyword(Keyword.FLYING, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Denman Rooke"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7dc61e4-46e3-473b-90f8-27e71e1088e8.jpg?1783929424"
    }
}
