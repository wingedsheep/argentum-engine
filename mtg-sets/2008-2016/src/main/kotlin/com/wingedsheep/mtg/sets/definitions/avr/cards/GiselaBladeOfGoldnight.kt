package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Gisela, Blade of Goldnight — Avacyn Restored #209
 * {4}{R}{W}{W} · Legendary Creature — Angel · 5/5
 *
 * Flying, first strike
 * If a source would deal damage to an opponent or a permanent an opponent controls, that source
 * deals double that damage to that player or permanent instead.
 * If a source would deal damage to you or a permanent you control, prevent half that damage,
 * rounded up.
 *
 * Both halves read any source, Gisela's own controller's included. The second is a prevention
 * effect, so "damage can't be prevented" lets it through in full.
 */
val GiselaBladeOfGoldnight = card("Gisela, Blade of Goldnight") {
    manaCost = "{4}{R}{W}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying, first strike\n" +
        "If a source would deal damage to an opponent or a permanent an opponent controls, that source " +
        "deals double that damage to that player or permanent instead.\n" +
        "If a source would deal damage to you or a permanent you control, prevent half that damage, rounded up."
    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.OpponentOrPermanentTheyControl)
        )
    )
    replacementEffect(
        PreventDamage(
            half = true,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyOf(listOf(Recipient.You, Recipient.PermanentYouControl))
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "209"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7208fcb8-83e6-44ce-af9a-ca1566825018.jpg?1783940656"
    }
}
