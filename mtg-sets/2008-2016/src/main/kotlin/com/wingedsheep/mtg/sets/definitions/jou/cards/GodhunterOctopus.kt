package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Godhunter Octopus
 * {5}{U}
 * Creature — Octopus
 * 5/5
 * This creature can't attack unless defending player controls an enchantment or an enchanted
 * permanent.
 *
 * The Deep-Sea Serpent restriction with a two-branch filter. "Enchanted permanent" is a permanent
 * the defending player controls with an Aura attached, whoever controls that Aura — so the
 * enchanted branch carries [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEnchanted]
 * on its own, and the OR builds a union rather than flattening the card types.
 */
val GodhunterOctopus = card("Godhunter Octopus") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus"
    power = 5
    toughness = 5
    oracleText = "This creature can't attack unless defending player controls an enchantment or an enchanted permanent."

    staticAbility {
        ability = CantAttackUnless(
            Exists(
                Player.DefendingPlayer,
                Zone.BATTLEFIELD,
                GameObjectFilter.Enchantment or GameObjectFilter.Permanent.enchanted()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Tyler Jacobson"
        flavorText = "\"I will match Thassa drop for drop and show a god what true power is.\"\n—Kiora"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da106c23-fd1c-461b-9301-159f93ef489b.jpg?1783939448"
        ruling("2014-04-26", "Whether the defending player controls an enchantment or enchanted permanent is checked only as attackers are declared. Once Godhunter Octopus has been declared as an attacker, it will continue to attack even if the defending player loses control of each enchantment or enchanted permanent they controlled.")
    }
}
