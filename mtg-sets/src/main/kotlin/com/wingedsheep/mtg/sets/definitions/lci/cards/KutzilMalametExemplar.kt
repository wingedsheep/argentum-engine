package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kutzil, Malamet Exemplar — The Lost Caverns of Ixalan #232 (canonical printing)
 * {1}{G}{W} · Legendary Creature — Cat Warrior · 3/3
 *
 * Your opponents can't cast spells during your turn.
 * Whenever one or more creatures you control each with power greater than its base power deals
 * combat damage to a player, draw a card.
 *
 * Two reusable primitives:
 *  - The cast restriction is [PlayersCantCastSpells] (`EachOpponent`, `condition = IsYourTurn`) —
 *    the same continuous cast-legality restriction Grand Abolisher / Voice of Victory use. It only
 *    stops opponents while it's Kutzil's controller's turn; it never restricts the controller, and
 *    never restricts opponents on their own turns.
 *  - The draw is an [OneOrMoreDealCombatDamageToPlayerEvent] batch trigger (CR 603.2c — fires once
 *    per combat-damage step per damaged player, not once per creature) whose `sourceFilter` is
 *    `GameObjectFilter.Creature.powerGreaterThanBase()`: each qualifying creature's current
 *    (projected) power must exceed its own printed base power. A +1/+1 counter or an anthem both
 *    raise power above base and qualify; an unmodified 3/3 does not. "You control" is implicit in
 *    the batch grouping (events are grouped by the damage source's controller and matched to this
 *    observer's controller). Draws exactly one card regardless of how many qualifying creatures
 *    connected.
 */
val KutzilMalametExemplar = card("Kutzil, Malamet Exemplar") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Cat Warrior"
    power = 3
    toughness = 3
    oracleText = "Your opponents can't cast spells during your turn.\n" +
        "Whenever one or more creatures you control each with power greater than its base power " +
        "deals combat damage to a player, draw a card."

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    triggeredAbility {
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.powerGreaterThanBase()
            ),
            TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Marie Magny"
        flavorText = "\"Invaders in the West Chasm? Give me an hour and they'll be fungus food.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9f88a40-a6ed-4c1f-a309-011aca1acddd.jpg?1782694425"
    }
}
