package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Necromantic Summons
 * {4}{B}
 * Sorcery
 *
 * Put target creature card from a graveyard onto the battlefield under your control.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * that creature enters with two additional +1/+1 counters on it.
 *
 * The counters are an entry counter on the move itself, so the creature *enters* with them (its
 * own enters triggers and static abilities see them), not a put-counters-after. Their count is a
 * spell-mastery [Conditions.CardsInGraveyardMatchingAtLeast] read as the spell resolves, before
 * the card moves; Necromantic Summons is still on the stack then, so it never counts itself.
 */
val NecromanticSummons = card("Necromantic Summons") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Put target creature card from a graveyard onto the battlefield under your control.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "that creature enters with two additional +1/+1 counters on it."

    spell {
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(
            creature,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD,
            entryCounters = listOf(
                EntryCounter(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    count = DynamicAmounts.conditional(
                        Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                        ifTrue = 2,
                        ifFalse = 0,
                    ),
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3afe7891-09d5-470e-aa41-71d89249875b.jpg?1783938339"
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
