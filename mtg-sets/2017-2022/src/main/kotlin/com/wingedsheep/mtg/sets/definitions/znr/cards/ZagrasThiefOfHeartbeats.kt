package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zagras, Thief of Heartbeats — Zendikar Rising #241
 * {4}{B}{R} · Legendary Creature — Vampire Rogue · 4/4
 *
 * This spell costs {1} less to cast for each creature in your party.
 * Flying, deathtouch, haste
 * Other creatures you control have deathtouch.
 * Whenever a creature you control deals combat damage to a planeswalker, destroy that planeswalker.
 *
 * The cost line is the Sea Gate Colossus party reduction. The last line is Zagras's own observer
 * trigger: its source filter binds [EffectTarget.TriggeringEntity] to the creature that dealt the
 * damage, so "that planeswalker" is [EffectTarget.DamageRecipient].
 */
val ZagrasThiefOfHeartbeats = card("Zagras, Thief of Heartbeats") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Vampire Rogue"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "Flying, deathtouch, haste\n" +
        "Other creatures you control have deathtouch.\n" +
        "Whenever a creature you control deals combat damage to a planeswalker, destroy that planeswalker."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH, Keyword.HASTE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl())
            .dealsCombatDamage(Recipient.Object(GameObjectFilter.Planeswalker))
        effect = Effects.Destroy(EffectTarget.DamageRecipient)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac81448c-f87b-4ca1-b951-b359216ab1d0.jpg?1783929317"
        ruling(
            "2020-09-25",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying, add any cost increases, then apply any cost reductions. The mana value of the spell " +
                "is determined only by its mana cost, no matter what the total cost to cast the spell was."
        )
    }
}
