package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thran Vigil — The Brothers' War #114
 * {1}{B} · Enchantment · Uncommon
 *
 * Whenever one or more artifact and/or creature cards leave your graveyard during your turn,
 * put a +1/+1 counter on target creature you control.
 *
 * "One or more … leave" is CR 603.2c batch wording: the batching
 * `Triggers.oneOrMore(filter).leaveYourGraveyard()` fires once per batch however many matching
 * cards moved (Desecrated Tomb, Insidious Roots). "During your turn" is the trigger restriction
 * `Conditions.IsYourTurn`, as on Attuned Hunter.
 */
val ThranVigil = card("Thran Vigil") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more artifact and/or creature cards leave your graveyard during your turn, " +
        "put a +1/+1 counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Artifact or GameObjectFilter.Creature).leaveYourGraveyard()
        triggerRestriction = Conditions.IsYourTurn
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Jinho Bae"
        flavorText = "Glacian built his automatons to last, and last they did, beyond even the Thran themselves."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3faed400-0712-4fc1-b710-144db40a5e11.jpg?1783920081"
    }
}
