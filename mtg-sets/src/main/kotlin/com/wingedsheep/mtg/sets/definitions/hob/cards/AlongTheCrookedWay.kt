package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Along the Crooked Way — The Hobbit #60
 * {2}{B} · Enchantment · Rare
 *
 * When this enchantment enters, return target creature card from your graveyard to your hand.
 * Whenever a creature card leaves your graveyard, amass Goblins 1.
 * {1}{B}: Goblins and Orcs you control gain menace until end of turn.
 *
 * Modeling notes:
 *  - The middle ability is a graveyard-exit trigger, not a battlefield one: the event pattern is
 *    `ZoneChangeEvent(from = GRAVEYARD)` with no `to`, so *any* destination (hand, exile, library,
 *    battlefield, stack — a flashback/escape cast counts) fires it. `.youControl()` scopes it to
 *    "your graveyard"; for a card leaving a non-battlefield zone the matcher has no last-known
 *    controller and falls back to the card's owner, which is exactly whose graveyard it was in
 *    (CR 400.3).
 *  - The enchantment's own enters trigger moves a creature card out of your graveyard, so it
 *    feeds the amass trigger — the two abilities are deliberately chained on the printed card.
 *  - "Goblins and Orcs you control" is one OR over subtypes, and it covers the amassed Army:
 *    an Army that has been amassed with Goblins is itself a Goblin (CR 701.47a).
 */
val AlongTheCrookedWay = card("Along the Crooked Way") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, return target creature card from your graveyard " +
        "to your hand.\n" +
        "Whenever a creature card leaves your graveyard, amass Goblins 1.\n" +
        "{1}{B}: Goblins and Orcs you control gain menace until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creatureCard = target("target creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Move(creatureCard, Zone.HAND)
        description = "Return target creature card from your graveyard to your hand."
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(filter = GameObjectFilter.Creature, from = Zone.GRAVEYARD),
            binding = TriggerBinding.ANY
        ).youControl()
        effect = Effects.Amass(1, "Goblin")
        description = "Amass Goblins 1."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.youControl().withAnySubtype("Goblin", "Orc")),
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
        )
        description = "Goblins and Orcs you control gain menace until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3696d65c-fffd-4685-bb2d-e8769bf476e3.jpg?1785412571"
    }
}
