package com.wingedsheep.mtg.sets.definitions.wildsofeldraineset.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Spellbook Vendor
 * {1}{W}
 * Creature — Human Peasant
 * 2/2
 *
 * Vigilance
 * At the beginning of combat on your turn, you may pay {1}. When you do, create a Sorcerer
 * Role token attached to target creature you control.
 */
val SpellbookVendor = card("Spellbook Vendor") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 2
    oracleText = "Vigilance\nAt the beginning of combat on your turn, you may pay {1}. When you do, create a Sorcerer Role token attached to target creature you control. (If you control another Role on it, put that one into the graveyard. Enchanted creature gets +1/+1 and has \"Whenever this creature attacks, scry 1.\")"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val targetCreature = target("target creature you control", Targets.CreatureYouControl)
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.CreateRoleToken("Sorcerer Role", targetCreature)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ceac5b5-05eb-4f00-9477-3db490be24dd.jpg?1692936729"
        ruling("2023-09-01", "You don't choose a target for Spellbook Vendor's ability at the time it triggers. Rather, a second reflexive ability triggers when you pay this way. You choose a target for that ability as it goes on the stack.")
        ruling("2023-09-01", "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and the enchant creature ability.")
    }
}
