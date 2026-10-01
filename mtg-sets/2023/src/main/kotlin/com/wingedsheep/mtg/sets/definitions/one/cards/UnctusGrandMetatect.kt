package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unctus, Grand Metatect
 * {1}{U}{U}
 * Legendary Artifact Creature — Phyrexian Vedalken
 * 2/4
 * Other blue creatures you control have "Whenever this creature becomes tapped, draw a card, then
 * discard a card."
 * Other artifact creatures you control get +1/+1.
 * {U/P}: Until end of turn, target creature you control becomes a blue artifact in addition to its
 * other colors and types. Activate only as a sorcery.
 *
 * The granted loot reads the blue filter off projected state, so a creature the activated ability
 * turns blue gains the trigger (and the +1/+1, being an artifact now) for the rest of the turn.
 */
val UnctusGrandMetatect = card("Unctus, Grand Metatect") {
    manaCost = "{1}{U}{U}"
    typeLine = "Legendary Artifact Creature — Phyrexian Vedalken"
    oracleText = "Other blue creatures you control have \"Whenever this creature becomes tapped, draw a card, then discard a card.\"\n" +
        "Other artifact creatures you control get +1/+1.\n" +
        "{U/P}: Until end of turn, target creature you control becomes a blue artifact in addition to its other colors and types. Activate only as a sorcery. ({U/P} can be paid with either {U} or 2 life.)"
    power = 2
    toughness = 4

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.becomesTapped(),
                effect = Patterns.Hand.loot()
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE).youControl(), excludeSelf = true)
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.ArtifactCreature.youControl(), excludeSelf = true)
        )
    }

    activatedAbility {
        val creature = target(TargetFilter.CreatureYouControl)
        cost = Costs.Mana("{U/P}")
        effect = Effects.AddColor(Color.BLUE, creature, Duration.EndOfTurn) then
            Effects.AddCardType("ARTIFACT", creature, Duration.EndOfTurn)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/164b07e6-48ba-4789-bd8f-7cada1fec8a9.jpg?1783918054"
    }
}
