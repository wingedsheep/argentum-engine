package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Seam Rip
 * {W}
 * Enchantment
 * When this enchantment enters, exile target nonland permanent an opponent controls
 * with mana value 2 or less until this enchantment leaves the battlefield.
 */
val SeamRip = card("Seam Rip") {
    manaCost = "{W}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls with mana value 2 or less until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "nonland permanent an opponent controls with mana value 2 or less",
            TargetPermanent(filter = TargetFilter.NonlandPermanentOpponentControls.manaValueAtMost(2))
        )
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Sam Guay"
        flavorText = "Drix hunt down Eldrazi Potentiate without hesitation or explanation."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d298847-2d02-4593-b4d3-c5b722edac1e.jpg?1752946687"
    }
}
