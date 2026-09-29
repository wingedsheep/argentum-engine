package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Muraganda // Primordial Plasm — March of the Machine #192.
 * {4}{G} · Battle — Siege · defense 6 // Creature — Ooze 4/4
 *
 * Front: +1/+1 counter on target creature you control, then it fights up to one target creature
 * you don't control (an optional second target; fight does nothing if either target is gone).
 * Back: at the beginning of combat on your turn, another target creature gets +2/+2 and loses all
 * abilities until end of turn.
 */
private val InvasionOfMuragandaFront = card("Invasion of Muraganda") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Battle — Siege"
    startingDefense = 6
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, put a +1/+1 counter on target creature you control. Then that " +
        "creature fights up to one target creature you don't control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, mine) then
            Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93345804-dda4-42a2-84d4-8fcd376dd2d4.jpg?1783916973"
        ruling("2023-04-14", "You can choose only a creature you control as a target for Invasion of Muraganda's enters-the-battlefield ability.")
        ruling("2023-04-14", "If the creature you control is an illegal target as the ability tries to resolve, you won't put a +1/+1 counter on it. If that creature is a legal target but the other creature isn't, you'll still put the +1/+1 counter on the creature you control, but neither creature will deal or be dealt damage.")
    }
}

private val PrimordialPlasm = card("Primordial Plasm") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Ooze"
    power = 4
    toughness = 4
    oracleText = "At the beginning of combat on your turn, another target creature gets +2/+2 and " +
        "loses all abilities until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.OtherCreature)
        effect = Effects.ModifyStats(2, 2, t) then Effects.RemoveAllAbilities(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Adam Paquette"
        flavorText = "Before the tree of life branched into predators and prey, there was only shapeless hunger."
        imageUri = "https://cards.scryfall.io/normal/back/9/3/93345804-dda4-42a2-84d4-8fcd376dd2d4.jpg?1783916973"
        ruling("2023-04-14", "A creature that loses all abilities because of Primordial Plasm's ability may later gain abilities.")
        ruling("2023-04-14", "If the creature affected by Primordial Plasm's ability had an ability defining its power and/or toughness, that base value will become 0. In many situations, having a base toughness of 0 would be a problem, but Primordial Plasm's ability helpfully provides +2/+2, so the creature should survive.")
    }
}

val InvasionOfMuraganda: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfMuragandaFront,
    backFace = PrimordialPlasm,
)
