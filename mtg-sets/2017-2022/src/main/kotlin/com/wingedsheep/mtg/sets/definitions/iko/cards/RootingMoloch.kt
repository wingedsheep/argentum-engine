package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rooting Moloch
 * {4}{R}
 * Creature — Lizard
 * 4/4
 *
 * When this creature enters, exile target card with a cycling ability from your graveyard. Until
 * the end of your next turn, you may play that card.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * The ETB is the targeted-impulse pipeline (Thor, God of Thunder): gather the chosen graveyard
 * card, exile it tracked, and grant the play permission on the moved card. "With a cycling
 * ability" is `withCycling()` — typecycling counts too (CR 702.29e).
 */
val RootingMoloch = card("Rooting Moloch") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, exile target card with a cycling ability from your " +
        "graveyard. Until the end of your next turn, you may play that card.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(
            TargetFilter(
                baseFilter = GameObjectFilter.Any.withCycling().ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(targeted, CardDestination.ToZone(Zone.EXILE))
            run(Effects.GrantMayPlayFromExile(exiled, MayPlayExpiry.UntilEndOfNextTurn))
        }
        description = "When this creature enters, exile target card with a cycling ability from " +
            "your graveyard. Until the end of your next turn, you may play that card."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Andrey Kuzinskiy"
        flavorText = "Their feeding habits leave them with gizzards full of tumbled gems."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc2fd581-5cf8-4154-90dd-922afddcd556.jpg?1783931044"
    }
}
