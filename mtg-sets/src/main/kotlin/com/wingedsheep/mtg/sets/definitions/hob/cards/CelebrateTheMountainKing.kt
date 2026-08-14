package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Celebrate the Mountain-king
 * {3}{W}
 * Enchantment
 *
 * When this enchantment enters, for each opponent, exile up to one target nonland permanent that
 * player controls until this enchantment leaves the battlefield.
 * When this enchantment enters, recruit.
 *
 * Two separate enters triggers, exactly as printed — they go on the stack together and their
 * controller orders them, so the recruit can be resolved before or after the exile.
 *
 * "For each opponent, … up to one target … that player controls" follows the corpus convention for
 * this wording (Blatant Thievery, Riptide Gearhulk, Omega, Heartless Evolution): one *optional*
 * target restricted to a nonland permanent an opponent controls — exactly right in 1v1, and the
 * shape the multiplayer per-opponent targeting work generalizes. `optional = true` carries the "up
 * to one", so declining is legal and the trigger still resolves.
 *
 * The exile half is the Banishing Light pair: [Effects.ExileUntilLeaves] links the exiled card to
 * this enchantment, and a [Triggers.LeavesBattlefield] trigger returns the linked pile under its
 * owner's control. Bouncing or destroying the enchantment in response to its own enters trigger
 * gives the usual O-Ring result — the leaves trigger resolves with an empty linked pile and the
 * exile then never happens.
 */
val CelebrateTheMountainKing = card("Celebrate the Mountain-king") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, for each opponent, exile up to one target nonland " +
        "permanent that player controls until this enchantment leaves the battlefield.\n" +
        "When this enchantment enters, recruit. (Draw a card, then discard a card. If you " +
        "discarded a nonland card, create a 1/1 white Human Soldier creature token.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val exiled = target(
            "up to one target nonland permanent that player controls",
            TargetPermanent(count = 1, optional = true, filter = TargetFilter.NonlandPermanentOpponentControls)
        )
        effect = Effects.ExileUntilLeaves(exiled)
        description = "When this enchantment enters, for each opponent, exile up to one target " +
            "nonland permanent that player controls until this enchantment leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42fbd61d-e1a6-465d-b1a3-f5ee0869d3af.jpg?1785496910"
    }
}
