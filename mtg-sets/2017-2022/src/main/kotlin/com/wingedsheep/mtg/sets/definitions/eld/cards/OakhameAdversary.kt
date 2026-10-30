package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Oakhame Adversary
 * {3}{G}
 * Creature — Elf Warrior
 * 2/3
 *
 * This spell costs {2} less to cast if an opponent controls a green permanent.
 * Deathtouch
 * Whenever this creature deals combat damage to a player, draw a card.
 */
val OakhameAdversary = card("Oakhame Adversary") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "This spell costs {2} less to cast if an opponent controls a green permanent.\n" +
        "Deathtouch\n" +
        "Whenever this creature deals combat damage to a player, draw a card."
    power = 2
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.OpponentControls(GameObjectFilter.Permanent.withColor(Color.GREEN))
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Nicholas Gregory"
        flavorText = "The elves remember every secret path from the days when they ruled the realm."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fb4e77f-e189-4ad3-9fca-8da04289e396.jpg?1783932606"
    }
}
