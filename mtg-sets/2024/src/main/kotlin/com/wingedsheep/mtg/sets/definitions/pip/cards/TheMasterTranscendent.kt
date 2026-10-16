package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Master, Transcendent — reanimation in Tato Farmer's shape (any graveyard, milled this turn,
 * under your control), followed by three `Duration.Permanent` floating effects keyed to the
 * returned object: colour set to green (layer 5), creature types set to Mutant (layer 4) and base
 * P/T set to 3/3 (layer 7b, which overrides a P/T-defining CDA per the rulings). Card types are
 * untouched, so an artifact creature stays an artifact. The effects end when the object leaves
 * the battlefield (CR 400.7).
 */
val TheMasterTranscendent = card("The Master, Transcendent") {
    manaCost = "{1}{B}{G}{U}"
    typeLine = "Legendary Artifact Creature — Mutant"
    power = 2
    toughness = 4
    oracleText = "When The Master enters, target player gets two rad counters.\n" +
        "{T}: Put target creature card in a graveyard that was milled this turn onto the battlefield under your control. " +
        "It's a green Mutant with base power and toughness 3/3. (It loses its other colors and creature types.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.AddCounters(CounterType.RAD, 2, player)
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter(GameObjectFilter.Creature.milledThisTurn(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature, underYourControl = true) then
            Effects.ChangeColor(creature, setOf(Color.GREEN), Duration.Permanent) then
            Effects.SetCreatureSubtypes(setOf(Subtype.MUTANT.value), creature, Duration.Permanent) then
            Effects.SetBasePowerAndToughness(3, 3, creature, Duration.Permanent)
        description = "Put target creature card in a graveyard that was milled this turn onto the battlefield " +
            "under your control. It's a green Mutant with base power and toughness 3/3."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b0497da-670a-405c-a39c-97cae7942836.jpg?1783912421"
        ruling(
            "2024-03-08",
            "If a player is instructed to put a card into their graveyard from their library without using the word \"mill,\" (by the effect of Rowan's Grim Search from Wilds of Eldraine®, for example), that card isn't a legal target for The Master's last ability."
        )
        ruling(
            "2024-03-08",
            "The Master's last ability will override any characteristic-defining abilities the creature has that set its power and/or toughness. For example, if you put a Robobrain War Mind onto the battlefield with The Master's last ability, its base power and toughness will be 3/3, and the number of cards in your hand won't affect its power at all."
        )
        ruling(
            "2024-03-08",
            "Effects that modify a creature's power and/or toughness will still apply to the creature as normal. The same is true for any counters that change its power and/or toughness."
        )
    }
}
