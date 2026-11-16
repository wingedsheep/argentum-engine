package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Hedonist's Trove
 * {5}{B}{B}
 * Enchantment
 *
 * When this enchantment enters, exile target opponent's graveyard.
 * You may play lands from among cards exiled with this enchantment.
 * You may cast spells from among cards exiled with this enchantment. You can't cast more than one
 * spell this way each turn.
 *
 * - The enters trigger exiles the whole graveyard with `linkToSource = true`, so "cards exiled with
 *   this enchantment" is exactly this object's pile (CR 607): a later Trove — even the same card
 *   returning — is a new object with an empty pile, and the pile is cleared when it leaves.
 * - The two permissions are separate abilities with separate limits, and the ruling says the
 *   once-a-turn cap is for nonland cards only. So the spell half is a linked-exile cast grant over
 *   nonland cards with `oncePerTurn`, while the land half is an unlimited `MayPlayCardsFromExile`
 *   restricted to lands exiled with this source (the Valki / Tibalt emblem shape). A land play goes
 *   through the latter, which spends no allowance, so playing a land never closes the spell cast
 *   and vice versa. Land plays still use the turn's normal land drop.
 * - Mana is spent as normal: neither grant carries an "any type" rider.
 */
val HedonistsTrove = card("Hedonist's Trove") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target opponent's graveyard.\n" +
        "You may play lands from among cards exiled with this enchantment.\n" +
        "You may cast spells from among cards exiled with this enchantment. You can't cast more than " +
        "one spell this way each turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val troveGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, opponent.asPlayer))
            exile(troveGraveyard, opponent.asPlayer, linkToSource = true)
        }
        description = "When this enchantment enters, exile target opponent's graveyard."
    }

    staticAbility {
        ability = MayPlayCardsFromExile(filter = GameObjectFilter.Land.exiledWithSource())
    }

    staticAbility {
        ability = GrantMayCastFromLinkedExile(
            filter = GameObjectFilter.Nonland,
            oncePerTurn = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2b73bbf-39a8-4358-9af6-8f4f0b57c028.jpg?1783938596"
        ruling("2015-02-25", "The cards are exiled face up.")
        ruling("2015-02-25", "The once-a-turn restriction of the last ability applies only to nonland cards. You can play a land card exiled with Hedonist's Trove on the same turn you cast a nonland card exiled that way.")
        ruling("2015-02-25", "Playing a card this way follows the normal rules for playing the card. You must pay its costs, and you must follow all applicable timing rules. For example, if one of the cards is a creature card, you can cast that card only during your main phase while the stack is empty.")
        ruling("2015-02-25", "Under normal circumstances, you can play a land card exiled with Hedonist's Trove only if you haven't played a land yet that turn.")
        ruling("2015-02-25", "If Hedonist's Trove leaves the battlefield, any cards it exiled remain exiled. Those cards can no longer be played. Any future instance of Hedonist's Trove (even one represented by the same card) will be a new object and won't allow you to play those cards.")
    }
}
