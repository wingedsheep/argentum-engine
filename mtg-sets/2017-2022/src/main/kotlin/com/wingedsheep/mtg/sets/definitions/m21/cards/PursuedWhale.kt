package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pursued Whale — Core Set 2021 #60
 * {5}{U}{U} · Creature — Whale · 8/8
 *
 * When this creature enters, each opponent creates a 1/1 red Pirate creature token with "This token
 * can't block" and "Creatures you control attack each combat if able."
 * Spells your opponents cast that target this creature cost {3} more to cast.
 *
 * Each opponent controls their own Pirate (the Slaughter Specialist shape), so the token's "you" in
 * "Creatures you control attack each combat if able" is that opponent — a [MustAttack] over
 * [GroupFilter.AllCreaturesYouControl] read relative to the token's controller. The tax is Charix's
 * [SpellCostTarget.OpponentsCastTargeting] shape.
 */
val PursuedWhale = card("Pursued Whale") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    power = 8
    toughness = 8
    oracleText = "When this creature enters, each opponent creates a 1/1 red Pirate creature token with " +
        "\"This token can't block\" and \"Creatures you control attack each combat if able.\"\n" +
        "Spells your opponents cast that target this creature cost {3} more to cast."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Pirate"),
            controller = EffectTarget.PlayerRef(Player.EachOpponent),
            staticAbilities = listOf(CantBlock(), MustAttack(GroupFilter.AllCreaturesYouControl)),
            imageUri = "https://cards.scryfall.io/normal/front/d/a/da3002d9-7754-4c7f-a051-382cd7a86e80.jpg?1783930590",
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
            modification = CostModification.IncreaseGeneric(3),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Jonathan Kuo"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1501118-5837-49b5-9446-0bc3032035ca.jpg?1783930724"
        ruling("2020-06-23", "If a creature affected by the Pirate token can't attack for any reason (such as being tapped or having come under that player's control that turn), then it doesn't attack. If there's a cost associated with having it attack, the player isn't forced to pay that cost, so it doesn't have to attack in that case either.")
        ruling("2020-06-23", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases (such as that of Pursued Whale), then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
    }
}
