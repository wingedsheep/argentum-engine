package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tyvar the Bellicose — March of the Machine: The Aftermath #48
 * {2}{B}{G} · Legendary Creature — Elf Warrior · 5/4
 *
 * Whenever one or more Elves you control attack, they gain deathtouch until end of turn.
 * Each creature you control has "Whenever a mana ability of this creature resolves, put a number
 * of +1/+1 counters on it equal to the amount of mana this creature produced. This ability
 * triggers only once each turn."
 *
 * "They" are the Elves the attack trigger captured at declaration. The granted trigger fires for
 * any mana ability of the creature — {T} or not, manual or auto-paid — and reads what that ability
 * produced; it isn't a mana ability itself, so it uses the stack.
 */
val TyvarTheBellicose = card("Tyvar the Bellicose") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 5
    toughness = 4
    oracleText = "Whenever one or more Elves you control attack, they gain deathtouch until end of turn.\n" +
        "Each creature you control has \"Whenever a mana ability of this creature resolves, put a number " +
        "of +1/+1 counters on it equal to the amount of mana this creature produced. This ability " +
        "triggers only once each turn.\""

    triggeredAbility {
        trigger = Triggers.you.attacks(with = GameObjectFilter.Creature.withSubtype(Subtype.ELF))
        effect = Effects.ForEachInCollection(
            CollectionSlot.TriggerCaptured,
            Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.IterationEntity)
        )
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.manaAbilityResolves(),
                effect = Effects.AddDynamicCounters(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.triggerManaProduced(),
                    EffectTarget.Self
                ),
                oncePerTurn = true
            ),
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "48"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01a543f0-72fe-4a30-a4a0-356971c1c68a.jpg?1783916508"
    }
}
