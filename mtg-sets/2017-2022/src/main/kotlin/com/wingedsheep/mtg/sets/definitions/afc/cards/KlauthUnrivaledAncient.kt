package com.wingedsheep.mtg.sets.definitions.afc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Klauth, Unrivaled Ancient
 * {5}{R}{G}
 * Legendary Creature — Dragon
 * 4/4
 * Flying, haste
 * Whenever Klauth attacks, add X mana in any combination of colors, where X is the total power of
 * attacking creatures. Spend this mana only to cast spells. Until end of turn, you don't lose this
 * mana as steps and phases end.
 *
 * X is read as the trigger resolves (ruling), summing the projected power of every attacking
 * creature. The mana is [ManaRestriction.SpellsOnly] and [ManaExpiry.KEPT_UNTIL_END_OF_TURN], so it
 * survives combat's step boundaries into the postcombat main phase.
 */
val KlauthUnrivaledAncient = card("Klauth, Unrivaled Ancient") {
    manaCost = "{5}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, haste\nWhenever Klauth attacks, add X mana in any combination of colors, where X is the " +
        "total power of attacking creatures. Spend this mana only to cast spells. Until end of turn, you don't " +
        "lose this mana as steps and phases end."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddManaInAnyCombination(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).sumPower(),
            restriction = ManaRestriction.SpellsOnly,
            expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "50"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f5cf828-17b2-4105-9a9a-ee0ab720c2ee.jpg?1783926239"
    }
}
