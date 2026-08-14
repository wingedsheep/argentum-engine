package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Roxxon Brutes — Marvel Super Heroes #113
 * {4}{B} · Creature — Human Berserker Villain · Common
 * 4/4
 *
 * Menace
 * Whenever you draw your second card each turn, put a +1/+1 counter on target creature.
 * Basic landcycling {2}
 *
 * The draw trigger is [Triggers.NthCardDrawn] (CR 121.2) — it reads the per-player draw counter and
 * fires exactly once per turn, on the crossing into the second draw, so a single two-card draw fires
 * it once rather than twice. Unlike Atlantean Cavalry's self-buff, the counter here goes on a
 * declared *target* creature (any creature, either controller), so the ability needs a target on the
 * stack; if that target is illegal on resolution the whole trigger is countered.
 *
 * Basic landcycling is the [KeywordAbility.basicLandcycling] variant of cycling — the engine's shared
 * typecycling machinery with a "basic land card" search filter.
 */
val RoxxonBrutes = card("Roxxon Brutes") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Berserker Villain"
    power = 4
    toughness = 4
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on target creature.\n" +
        "Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, " +
        "reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    keywordAbility(KeywordAbility.basicLandcycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66550490-74e1-4bf1-8741-1266dfab3a03.jpg?1783902938"
    }
}
