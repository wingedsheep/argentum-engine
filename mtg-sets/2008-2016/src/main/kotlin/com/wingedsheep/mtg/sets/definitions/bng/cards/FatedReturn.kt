package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fated Return — Born of the Gods #69 (canonical / earliest real printing)
 * {4}{B}{B}{B} · Instant
 *
 * Put target creature card from a graveyard onto the battlefield under your control. It gains
 * indestructible. If it's your turn, scry 2.
 *
 * Rise from the Grave's reanimation, then a [Duration.Permanent] indestructible grant (the ruling:
 * no duration, lasts until it leaves the battlefield, not a copiable value), then the Fated cycle's
 * "If it's your turn, scry 2" tail checked at resolution.
 */
val FatedReturn = card("Fated Return") {
    manaCost = "{4}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Put target creature card from a graveyard onto the battlefield under your control. " +
        "It gains indestructible. If it's your turn, scry 2. (Look at the top two cards of your " +
        "library, then put any number of them on the bottom of your library and the rest on top " +
        "in any order.)"

    spell {
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldUnderYourControl(creatureCard) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creatureCard, Duration.Permanent) then
            Effects.If(Conditions.IsYourTurn, Effects.Scry(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a55878ec-a903-4694-8709-e29fa88b2c28.jpg?1783939559"
        ruling("2014-02-01", "The effect that causes the creature to gain indestructible doesn’t have a duration. It lasts until the creature leaves the battlefield.")
        ruling("2014-02-01", "The indestructible granted by Fated Return isn’t part of the creature’s copiable values. If the creature is copied, the copy won’t have indestructible (unless the creature otherwise has indestructible).")
    }
}
