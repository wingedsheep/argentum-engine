package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Hawkeye, Young Avenger — Marvel Super Heroes #131 (uncommon)
 * {3}{R} · Legendary Creature — Human Archer Hero · 2/4
 *
 * Reach
 * If a source you control would deal noncombat damage to an opponent or a permanent an opponent
 * controls, instead it deals that much damage plus X, where X is Hawkeye's power.
 *
 * A [ModifyDamageAmount] replacement in the Fated Firepower shape: the additive bonus is dynamic
 * ([DynamicAmounts.sourcePower]), and `dynamicModifier` is evaluated against the *replacement's own
 * source* — so it reads Hawkeye's current (projected) power, not the damage source's. The
 * [EventPattern.DamageEvent] scopes it exactly as the oracle text does:
 * [SourceFilter.YouControl] for "a source you control", [RecipientFilter.OpponentOrPermanentTheyControl]
 * for "an opponent or a permanent an opponent controls", and [DamageType.NonCombat] for "noncombat
 * damage" — so combat damage from your creatures is untouched. Hawkeye's own noncombat damage is
 * amplified too; if he's no longer on the battlefield the replacement is gone with him.
 */
val HawkeyeYoungAvenger = card("Hawkeye, Young Avenger") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Archer Hero"
    power = 2
    toughness = 4
    oracleText = "Reach\n" +
        "If a source you control would deal noncombat damage to an opponent or a permanent an " +
        "opponent controls, instead it deals that much damage plus X, where X is Hawkeye's power."

    keywords(Keyword.REACH)

    replacementEffect(
        ModifyDamageAmount(
            dynamicModifier = DynamicAmounts.sourcePower(),
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.OpponentOrPermanentTheyControl,
                source = SourceFilter.YouControl,
                damageType = DamageType.NonCombat,
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Darius Zablockis"
        flavorText = "\"Not bad for a girl with no superpowers, huh?\"\n—Hawkeye, Kate Bishop"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cebccbb-dde2-41fe-bfdc-0a46ee185749.jpg?1783902930"
    }
}
