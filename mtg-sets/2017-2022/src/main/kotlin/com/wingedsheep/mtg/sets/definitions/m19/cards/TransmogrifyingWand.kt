package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Transmogrifying Wand
 * {3}
 * Artifact
 * This artifact enters with three charge counters on it.
 * {1}, {T}, Remove a charge counter from this artifact: Destroy target creature. Its controller
 * creates a 2/4 white Ox creature token. Activate only as a sorcery.
 *
 * "Its controller" is the targeted creature's controller ([EffectTarget.TargetController]), so the
 * Ox goes to whoever lost the creature, not to the Wand's controller.
 */
val TransmogrifyingWand = card("Transmogrifying Wand") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters with three charge counters on it.\n" +
        "{1}, {T}, Remove a charge counter from this artifact: Destroy target creature. " +
        "Its controller creates a 2/4 white Ox creature token. Activate only as a sorcery."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.CHARGE,
            count = 3,
            selfOnly = true,
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.CHARGE),
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.CreateToken(
            power = 2,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Ox"),
            controller = EffectTarget.TargetController,
            imageUri = "https://cards.scryfall.io/normal/front/8/f/8f1d69f9-d9d7-47a9-9236-351f81ffa9ae.jpg?1783934475",
        )
        timing = TimingRule.SorcerySpeed
        description = "{1}, {T}, Remove a charge counter from this artifact: Destroy target creature. " +
            "Its controller creates a 2/4 white Ox creature token. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b06b670-7238-4732-85fd-ac6abebea57f.jpg?1783934507"
    }
}
