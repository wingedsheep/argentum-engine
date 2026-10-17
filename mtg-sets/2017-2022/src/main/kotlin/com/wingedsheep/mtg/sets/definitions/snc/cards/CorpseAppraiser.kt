package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Corpse Appraiser
 * {U}{B}{R}
 * Creature — Vampire Rogue
 * 3/3
 *
 * When this creature enters, exile up to one target creature card from a graveyard. If a card is
 * put into exile this way, look at the top three cards of your library, then put one of those
 * cards into your hand and the rest into your graveyard.
 *
 * The exile is tracked ([moveTracked]) so the dig is gated on a card actually reaching exile —
 * no target chosen, or a target that left the graveyard before resolution, skips the dig.
 */
val CorpseAppraiser = card("Corpse Appraiser") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Creature — Vampire Rogue"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, exile up to one target creature card from a graveyard. " +
        "If a card is put into exile this way, look at the top three cards of your library, then put " +
        "one of those cards into your hand and the rest into your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.CreatureInGraveyard, optional = true)
        effect = Effects.Pipeline {
            val appraised = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(appraised, CardDestination.ToZone(Zone.EXILE))
            ifNotEmpty(exiled) {
                run(Patterns.Library.lookAtTopAndKeep(count = 3, keepCount = 1))
            }
        }
        description = "When this creature enters, exile up to one target creature card from a graveyard. " +
            "If a card is put into exile this way, look at the top three cards of your library, then put " +
            "one of those cards into your hand and the rest into your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Josh Hass"
        flavorText = "\"Scarcely a mark. A true work of art!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0f254bb-ec77-4147-ab9c-559bc7fba8cd.jpg?1783923090"
    }
}
