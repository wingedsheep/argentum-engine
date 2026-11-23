package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frontline Medic
 * {2}{W}
 * Creature — Human Cleric
 * 3/3
 * Battalion — Whenever this creature and at least two other creatures attack, creatures you control
 * gain indestructible until end of turn.
 * Sacrifice this creature: Counter target spell with {X} in its mana cost unless its controller pays {3}.
 *
 * `hasXInManaCost()` reads the printed cost's `{X}` symbol, so a spell cast with X = 0 is still a
 * legal target, and a spell without {X} never is.
 */
val FrontlineMedic = card("Frontline Medic") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, creatures you " +
        "control gain indestructible until end of turn.\nSacrifice this creature: Counter target spell with " +
        "{X} in its mana cost unless its controller pays {3}."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    activatedAbility {
        target(TargetFilter(GameObjectFilter.Any.hasXInManaCost(), zone = Zone.STACK))
        cost = Costs.SacrificeSelf
        effect = Effects.CounterUnlessPays("{3}")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/711a5eca-531d-4b07-b7df-9b06bad491be.jpg?1783940143"
    }
}
