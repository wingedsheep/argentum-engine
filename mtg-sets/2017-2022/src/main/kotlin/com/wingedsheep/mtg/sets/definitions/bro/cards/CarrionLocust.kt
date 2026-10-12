package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Carrion Locust
 * {2}{B}
 * Creature — Insect Horror
 * 2/1
 * Flying
 * When this creature enters, exile target card from an opponent's graveyard. If it was a
 * creature card, that player loses 1 life.
 *
 * Scavenging Ooze's "exile, then check the exiled card's type" shape: the type gate reads the card
 * in exile via [Conditions.TargetIsCreatureCard]. "That player" is the exiled card's owner — the
 * opponent whose graveyard it was in — named by [Player.OwnerOf] (Nezumi Graverobber shape).
 */
val CarrionLocust = card("Carrion Locust") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect Horror"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, exile target card from an opponent's graveyard. " +
        "If it was a creature card, that player loses 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetIsCreatureCard(0),
                then = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.OwnerOf("target card"))),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Nino Is"
        flavorText = "No honor. No glory. No leftovers."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6759acb8-82c0-479f-86f3-f22ca7a2006c.jpg?1783920095"
    }
}
