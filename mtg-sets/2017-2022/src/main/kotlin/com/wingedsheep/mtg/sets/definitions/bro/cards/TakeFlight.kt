package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Take Flight
 * {3}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+0 and has flying and "Whenever this creature attacks, draw a card."
 *
 * The attack trigger is granted to the enchanted creature (Ghostly Touch's shape), so it fires
 * when that creature attacks and draws for its controller.
 */
val TakeFlight = card("Take Flight") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+0 and has flying and \"Whenever this creature attacks, draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 0)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }
    staticAbility {
        ability = GrantTriggeredAbility(
            grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                effect = Effects.DrawCards(1)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Joshua Cairos"
        flavorText = "Though neither brother cared to admit it, flying the ancient thopter was their last " +
            "happy moment together."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46a2108e-e45e-4b21-a03b-5f644b6043be.jpg?1783920104"
    }
}
