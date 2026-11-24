package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Izzet Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {U} or {R}.
 * {U}{R}: Until end of turn, this artifact becomes a 2/1 blue and red Elemental artifact creature.
 * Whenever this artifact deals combat damage to a player, you may draw a card. If you do, discard a card.
 *
 * Same shape as the other Keyrunes; the loot trigger is a plain self combat-damage trigger, which
 * can only fire while the Keyrune is animated.
 */
val IzzetKeyrune = card("Izzet Keyrune") {
    manaCost = "{3}"
    colorIdentity = "UR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U} or {R}.\n" +
        "{U}{R}: Until end of turn, this artifact becomes a 2/1 blue and red Elemental artifact creature.\n" +
        "Whenever this artifact deals combat damage to a player, you may draw a card. If you do, discard a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{U}{R}")
        effect = Effects.BecomeCreature(
            power = 2,
            toughness = 1,
            creatureTypes = setOf("Elemental"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.BLUE.name, Color.RED.name)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83e4e83b-cbb8-4efd-b6c4-4459a29177ac.jpg?1783940324"
    }
}
