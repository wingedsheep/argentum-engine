package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Moag // Bloomwielder Dryads — March of the Machine #237.
 * {2}{G}{W} · Battle — Siege · defense 5 // Creature — Dryad 3/3
 *
 * Front: the Siege's enter trigger puts a +1/+1 counter on each creature you control.
 * Back: ward {2}, and at the beginning of your end step a +1/+1 counter on target creature you
 * control.
 */
private val InvasionOfMoagFront = card("Invasion of Moag") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "When this Siege enters, put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e454bf31-5aa0-4109-a3f5-c3f9cc838682.jpg?1783916950"
    }
}

private val BloomwielderDryads = card("Bloomwielder Dryads") {
    manaCost = ""
    colorIdentity = "GW"
    colorIndicator = "GW"
    typeLine = "Creature — Dryad"
    power = 3
    toughness = 3
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {2}.)\n" +
        "At the beginning of your end step, put a +1/+1 counter on target creature you control."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "At the beginning of your end step, put a +1/+1 counter on target creature " +
            "you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Filip Burburan"
        flavorText = "Against the implacable will of Phyrexia, the dryads of Moag raised a shield of " +
            "living summer."
        imageUri = "https://cards.scryfall.io/normal/back/e/4/e454bf31-5aa0-4109-a3f5-c3f9cc838682.jpg?1783916950"
    }
}

val InvasionOfMoag: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfMoagFront,
    backFace = BloomwielderDryads,
)
