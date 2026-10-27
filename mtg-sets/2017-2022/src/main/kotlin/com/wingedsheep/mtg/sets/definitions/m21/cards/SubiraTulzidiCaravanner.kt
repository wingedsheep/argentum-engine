package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Subira, Tulzidi Caravanner
 * {2}{R}
 * Legendary Creature — Human Shaman
 * 2/3
 *
 * Haste
 * {1}: Another target creature with power 2 or less can't be blocked this turn.
 * {1}{R}, {T}, Discard your hand: Until end of turn, whenever a creature you control with power 2
 * or less deals combat damage to a player, draw a card.
 *
 * The {1} ability is Dwarven Nomad's shape with "another": the power restriction lives on the
 * target, so a creature pumped above 2 before resolution is an illegal target and the ability
 * doesn't resolve, while a pump after resolution doesn't undo the evasion (ruling).
 *
 * The last ability is Waltz of Rage's turn-scoped delayed trigger (`expiry = EndOfTurn`,
 * `fireOnce = false`); its power filter is read when the combat damage event is detected — i.e.
 * immediately after damage is dealt, as the ruling requires. Discarding an empty hand is a legal
 * cost payment (ruling).
 */
val SubiraTulzidiCaravanner = card("Subira, Tulzidi Caravanner") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Shaman"
    power = 2
    toughness = 3
    oracleText = "Haste\n" +
        "{1}: Another target creature with power 2 or less can't be blocked this turn.\n" +
        "{1}{R}, {T}, Discard your hand: Until end of turn, whenever a creature you control with " +
        "power 2 or less deals combat damage to a player, draw a card."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{1}")
        val creature = target(TargetFilter.OtherCreature.powerAtMost(2))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap, Costs.DiscardHand)
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.youControl().powerAtMost(2))
                .dealsCombatDamage(Recipient.AnyPlayer),
            expiry = DelayedTriggerExpiry.EndOfTurn,
            fireOnce = false,
            effect = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Leesha Hannigan"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/034b8d6d-95ea-434a-967a-e6675a7ce88a.jpg?1783930684"
        ruling(
            "2020-06-23",
            "If the target creature's power is greater than 2 as the first activated ability tries to resolve, the ability doesn't resolve. However, if instead the creature's power is raised above 2 after the ability resolves, it still can't be blocked that turn.",
        )
        ruling(
            "2020-06-23",
            "Once a creature with power 2 or less has been blocked, Subira's middle ability won't cause it to become unblocked.",
        )
        ruling("2020-06-23", "You can activate Subira's last ability if your hand is empty. You simply discard zero cards.")
        ruling(
            "2020-06-23",
            "Once Subira's last ability resolves, check to see if the delayed triggered ability should trigger based on the power of the creatures immediately after damage is dealt, not based on their power as the ability resolves.",
        )
    }
}
