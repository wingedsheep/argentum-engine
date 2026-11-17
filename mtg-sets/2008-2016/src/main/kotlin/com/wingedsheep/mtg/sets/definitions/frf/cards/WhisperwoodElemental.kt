package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Whisperwood Elemental
 * {3}{G}{G}
 * Creature — Elemental
 * 4/4
 * At the beginning of your end step, manifest the top card of your library.
 * Sacrifice this creature: Until end of turn, face-up nontoken creatures you control gain "When this
 * creature dies, manifest the top card of your library."
 *
 * The sacrifice ability's group is locked in as it resolves (ruling): `ForEachInGroup` visits only the
 * face-up nontoken creatures you control at that moment, so creatures that arrive later in the turn —
 * and the face-down ones already in play — never gain the dies trigger. Whisperwood itself is gone as a
 * cost, so it never grants the ability to itself.
 */
val WhisperwoodElemental = card("Whisperwood Elemental") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your end step, manifest the top card of your library. (Put it onto " +
        "the battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost if it's a " +
        "creature card.)\n" +
        "Sacrifice this creature: Until end of turn, face-up nontoken creatures you control gain \"When " +
        "this creature dies, manifest the top card of your library.\""

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Patterns.Library.manifest()
    }

    val manifestOnDeath = grantedTriggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.manifest()
        description = "When this creature dies, manifest the top card of your library."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.faceUp().nontoken().youControl()),
            Effects.GrantTriggeredAbility(manifestOnDeath, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66c61828-98b9-4264-b3dd-e83f881f4a67.jpg?1783938675"
        ruling(
            "2014-11-24",
            "The last ability grants that ability to face-up nontoken creatures you control as that ability " +
                "resolves. Creatures that come under your control later in the turn won't have that ability."
        )
    }
}
