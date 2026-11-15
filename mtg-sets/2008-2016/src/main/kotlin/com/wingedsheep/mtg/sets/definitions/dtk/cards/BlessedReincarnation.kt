package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blessed Reincarnation
 * {3}{U}
 * Instant
 *
 * Exile target creature an opponent controls. That player reveals cards from the top of their
 * library until a creature card is revealed. The player puts that card onto the battlefield, then
 * shuffles the rest into their library.
 * Rebound
 *
 * Polymorph's pipeline with exile in place of destroy, plus the bare [Keyword.REBOUND]. "That
 * player" is the exiled creature's controller: [Player.ControllerOf] and
 * [EffectTarget.TargetController] read its last-known controller once it has left the battlefield,
 * so a creature the opponent had stolen still makes *them* reveal. The revealed cards never leave
 * the library, so "shuffles the rest into their library" is a plain library shuffle after the
 * creature card is put onto the battlefield.
 */
val BlessedReincarnation = card("Blessed Reincarnation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile target creature an opponent controls. That player reveals cards from the top of their library until a creature card is revealed. The player puts that card onto the battlefield, then shuffles the rest into their library.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        val t = target(TargetFilter.CreatureOpponentControls)
        val thatPlayer = Player.ControllerOf("target creature")
        effect = Effects.Pipeline {
            run(Effects.Exile(t))
            val (found, allRevealed) = gatherUntilMatch(GameObjectFilter.Creature, player = thatPlayer)
            // Zone-transition reveal so the client shows the whole pile, including the creature
            // card that lands on the battlefield in the same update.
            reveal(allRevealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = thatPlayer))
            run(Effects.ShuffleLibrary(EffectTarget.TargetController))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/401cd79d-638e-43b4-8d0c-d7af05c55a1a.jpg?1783938610"
        ruling("2015-02-25", "If there are no creature cards in the library, it will be revealed and shuffled. The target creature will remain exiled.")
        ruling("2015-02-25", "Casting the card again due to the delayed triggered ability is optional. If you choose not to cast the card, or if you can't (perhaps because there are no legal targets available), the card will stay exiled. You won't get another chance to cast it on a future turn.")
    }
}
