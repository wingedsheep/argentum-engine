package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prismari Apprentice — Strixhaven: School of Mages #213 (canonical printing)
 * {U}{R} · Creature — Human Shaman · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature can't be
 * blocked this turn. If that spell has mana value 5 or greater, put a +1/+1 counter on this
 * creature.
 *
 * The "if" follows the first effect, so it is a resolution-time [Effects.If], not an
 * intervening-if: the evasion always happens. [Conditions.TriggeringSpellMatches] reads the
 * triggering spell (or copy) — on the stack an {X} counts as its chosen X.
 */
val PrismariApprentice = card("Prismari Apprentice") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature " +
        "can't be blocked this turn. If that spell has mana value 5 or greater, put a +1/+1 counter " +
        "on this creature."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self) then
            Effects.If(
                Conditions.TriggeringSpellMatches(GameObjectFilter.Any.manaValueAtLeast(5)),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this " +
            "creature can't be blocked this turn. If that spell has mana value 5 or greater, put a " +
            "+1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Igor Kieryluk"
        flavorText = "\"No one ever created great art by playing it safe!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dca3aa59-c8ac-4930-abf7-0a74d657122a.jpg?1783927301"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
