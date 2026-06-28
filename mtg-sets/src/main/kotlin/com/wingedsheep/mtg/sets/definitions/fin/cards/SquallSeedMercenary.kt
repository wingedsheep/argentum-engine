package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Squall, SeeD Mercenary — Final Fantasy #243
 * {2}{W}{B} · Legendary Creature — Human Knight Mercenary · 3/4
 *
 * Rough Divide — Whenever a creature you control attacks alone, it gains double strike until
 * end of turn.
 * Whenever Squall deals combat damage to a player, return target permanent card with mana
 * value 3 or less from your graveyard to the battlefield.
 *
 * "Rough Divide" is an ability word (CR 207.2c) — flavor only, no rules meaning. The first
 * ability is the Thoughtweft Imbuer "attacks alone" shape: an ANY-bound [Triggers.attacks]
 * with [AttackPredicate.Alone] over "creature you control", granting double strike to
 * [EffectTarget.TriggeringEntity] (the lone attacker).
 *
 * The second ability mirrors Rydia's Return's "permanent card in your graveyard" target —
 * `GameObjectFilter.Permanent.ownedByYou()` constrained with `manaValueAtMost(3)`, zone
 * GRAVEYARD — then returns it to the battlefield via [Effects.PutOntoBattlefield] (under its
 * owner's control, i.e. yours).
 */
val SquallSeedMercenary = card("Squall, SeeD Mercenary") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Knight Mercenary"
    power = 3
    toughness = 4
    oracleText = "Rough Divide — Whenever a creature you control attacks alone, it gains double strike " +
        "until end of turn.\n" +
        "Whenever Squall deals combat damage to a player, return target permanent card with mana value 3 " +
        "or less from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.GrantKeyword(
            Keyword.DOUBLE_STRIKE,
            EffectTarget.TriggeringEntity,
            Duration.EndOfTurn,
        )
        description = "Rough Divide — Whenever a creature you control attacks alone, it gains double " +
            "strike until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        target = TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Permanent.ownedByYou().manaValueAtMost(3),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.PutOntoBattlefield(EffectTarget.ContextTarget(0))
        description = "Whenever Squall deals combat damage to a player, return target permanent card with " +
            "mana value 3 or less from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Yuu Fujiki"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc4e5234-fb41-48f7-91f4-039710542bc3.jpg?1748706690"
    }
}
