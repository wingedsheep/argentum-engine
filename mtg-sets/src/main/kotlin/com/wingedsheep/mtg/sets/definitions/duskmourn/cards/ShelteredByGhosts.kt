package com.wingedsheep.mtg.sets.definitions.duskmourn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Sheltered by Ghosts
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature you control
 * When this Aura enters, exile target nonland permanent an opponent controls until
 * this Aura leaves the battlefield.
 * Enchanted creature gets +1/+0 and has lifelink and ward {2}.
 */
val ShelteredByGhosts = card("Sheltered by Ghosts") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\nWhen this Aura enters, exile target nonland permanent an opponent controls until this Aura leaves the battlefield.\nEnchanted creature gets +1/+0 and has lifelink and ward {2}."

    auraTarget = Targets.CreatureYouControl

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "nonland permanent an opponent controls",
            TargetPermanent(filter = TargetFilter.NonlandPermanentOpponentControls)
        )
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    staticAbility { ability = ModifyStats(1, 0) }
    staticAbility { ability = GrantKeyword(Keyword.LIFELINK) }
    staticAbility { ability = GrantWard(WardCost.Mana("{2}")) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/389f3f7b-be40-4a2d-b5cc-28471a577981.jpg?1726285971"
    }
}
