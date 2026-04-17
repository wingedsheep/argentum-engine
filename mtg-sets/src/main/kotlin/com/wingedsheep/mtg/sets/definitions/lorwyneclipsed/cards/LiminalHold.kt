package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Liminal Hold
 * {3}{W}
 * Enchantment
 * When this enchantment enters, exile up to one target nonland permanent an opponent
 * controls until this enchantment leaves the battlefield. You gain 2 life.
 */
val LiminalHold = card("Liminal Hold") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile up to one target nonland permanent an opponent controls until this enchantment leaves the battlefield. You gain 2 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "up to one nonland permanent an opponent controls",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanentOpponentControls)
        )
        effect = Effects.ExileUntilLeaves(permanent)
            .then(Effects.GainLife(2))
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Ovidio Cartagena"
        flavorText = "A reliquary is needed to pass through the eclipse safely. Without it, two warring selves will calcify into one."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5a40c16-7a5c-4ad1-be53-6b1b1be2affe.jpg?1767732486"
    }
}
