package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tamiyo, Compleated Sage — Kamigawa: Neon Dynasty #238
 * {2}{G}{G/U/P}{U} · Legendary Planeswalker — Tamiyo · Starting loyalty 5
 *
 * Compleated
 * +1: Tap up to one target artifact or creature. It doesn't untap during its controller's next
 *     untap step.
 * −X: Exile target nonland permanent card with mana value X from your graveyard. Create a token
 *     that's a copy of that card.
 * −7: Create Tamiyo's Notebook, a legendary colorless Book artifact token with "Spells you cast
 *     cost {2} less to cast" and "{T}: Draw a card."
 *
 * The −X target filter reads the loyalty X chosen at activation (`manaValueEqualsX`), and is
 * re-checked on resolution. Tamiyo's Notebook is the predefined token `PredefinedTokens.TamiyosNotebook`.
 */
val TamiyoCompleatedSage = card("Tamiyo, Compleated Sage") {
    manaCost = "{2}{G}{G/U/P}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Tamiyo"
    startingLoyalty = 5
    oracleText = "Compleated ({G/U/P} can be paid with {G}, {U}, or 2 life. If life was paid, this planeswalker enters with two fewer loyalty counters.)\n" +
        "+1: Tap up to one target artifact or creature. It doesn't untap during its controller's next untap step.\n" +
        "−X: Exile target nonland permanent card with mana value X from your graveyard. Create a token that's a copy of that card.\n" +
        "−7: Create Tamiyo's Notebook, a legendary colorless Book artifact token with \"Spells you cast cost {2} less to cast\" and \"{T}: Draw a card.\""

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(+1) {
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrArtifact), optional = true)
        effect = Effects.Tap(permanent) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                permanent,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    loyaltyAbilityX {
        val card = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.ownedByYou().manaValueEqualsX(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Exile(card) then Effects.CreateTokenCopyOfTarget(target = card)
        description = "Exile target nonland permanent card with mana value X from your graveyard. Create a token that's a copy of that card."
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePredefinedToken("Tamiyo's Notebook")
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "238"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/222a736e-d819-452d-aeda-eb848c4b2302.jpg?1783923828"
        ruling("2022-02-18", "A hybrid Phyrexian mana symbol is counted the same way as a normal mana symbol when determining the mana value of a card. Specifically, Tamiyo's mana value is always 5, even if you pay {2}{G}{U} and 2 life to cast her.")
        ruling("2022-02-18", "Compleated is a replacement effect, and it applies only to a permanent that is entering the battlefield with loyalty counters. Any other replacement effect that would apply to the number of loyalty counters it enters the battlefield with will apply as normal.")
        ruling("2022-02-18", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
    }
}
