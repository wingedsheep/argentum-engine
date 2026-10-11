package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Torrential Gearhulk — Kaladesh #67
 * {4}{U}{U} · Artifact Creature — Construct · 5/6
 *
 * Flash
 * When this creature enters, you may cast target instant card from your graveyard without paying
 * its mana cost. If that spell would be put into your graveyard, exile it instead.
 *
 * The cast happens while the trigger resolves, straight from the graveyard (Wishing Well's shape):
 * the targeted card is gathered and offered through [Effects.May] around
 * [Effects.CastFromCollectionWithoutPayingCost]; `insteadOfGraveyard = EXILE` stamps the
 * "exile it instead" rider on the spell, honoured on resolution and on counter/fizzle.
 */
val TorrentialGearhulk = card("Torrential Gearhulk") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 5
    toughness = 6
    oracleText = "Flash\n" +
        "When this creature enters, you may cast target instant card from your graveyard without " +
        "paying its mana cost. If that spell would be put into your graveyard, exile it instead."

    keywords(Keyword.FLASH)

    triggeredAbility {
        target(TargetFilter(GameObjectFilter.Instant.ownedByYou(), zone = Zone.GRAVEYARD))
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val instant = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(
                    from = instant,
                    insteadOfGraveyard = AfterResolveDestination.EXILE
                )
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "67"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d52868cb-087e-4f91-91bc-455f2e2e7cd7.jpg?1783937214"
        ruling("2016-09-20", "If you cast the card, you do so as part of the resolution of Torrential Gearhulk's triggered ability. You can't wait to cast it later in the turn. Timing restrictions (such as \"Cast [this card] only during combat\") still apply.")
        ruling("2016-09-20", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
