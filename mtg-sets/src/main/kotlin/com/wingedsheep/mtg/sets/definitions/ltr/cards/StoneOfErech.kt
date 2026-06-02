package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Stone of Erech
 * {1}
 * Legendary Artifact
 *
 * If a creature an opponent controls would die, exile it instead.
 * {2}, {T}, Sacrifice Stone of Erech: Exile target player's graveyard. Draw a card.
 */
val StoneOfErech = card("Stone of Erech") {
    manaCost = "{1}"
    typeLine = "Legendary Artifact"
    oracleText = "If a creature an opponent controls would die, exile it instead.\n" +
        "{2}, {T}, Sacrifice Stone of Erech: Exile target player's graveyard. Draw a card."

    // If a creature an opponent controls would die, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            )
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        target("target player", Targets.Player)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    storeAs = "targetGraveyard"
                ),
                MoveCollectionEffect(
                    from = "targetGraveyard",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
                ),
                Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Jonas De Ro"
        flavorText = "\"At the Stone of Erech they shall stand again and hear there a horn in the hills ringing.\"\n—Malbeth the Seer"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc02e193-df33-4eb1-adc1-b51ee931218a.jpg?1686970297"
    }
}
