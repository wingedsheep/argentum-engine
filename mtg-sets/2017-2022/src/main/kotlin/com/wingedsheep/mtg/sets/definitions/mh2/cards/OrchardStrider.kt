package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Orchard Strider — Modern Horizons 2 #169
 * {4}{G}{G} · Creature — Treefolk · 6 / 4
 *
 * When this creature enters, create two Food tokens. (They're artifacts with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 * Basic landcycling {1}{G} ({1}{G}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 *
 * [Effects.CreateFood] is the predefined-token facade, so the Food's own "{2}, {T}, Sacrifice this
 * token: You gain 3 life" ability comes from the shared token definition rather than being spelled
 * out here — the reminder text in the Oracle line is exactly that ability.
 *
 * Basic landcycling ([KeywordAbility.basicLandcycling]) narrows the shared typecycling search to
 * *basic* land cards, which is what makes a six-drop playable on turn two.
 */
val OrchardStrider = card("Orchard Strider") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 6
    toughness = 4
    oracleText = "When this creature enters, create two Food tokens. (They're artifacts with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Basic landcycling {1}{G} ({1}{G}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood(2)
    }

    keywordAbility(KeywordAbility.basicLandcycling("{1}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/386da156-59cd-4b96-9276-a7cb2ddd0421.jpg?1783926827"
    }
}
