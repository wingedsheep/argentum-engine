package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of the Swine — Theros #46 (canonical printing)
 * {X}{U}{U} · Sorcery
 *
 * Exile X target creatures. For each creature exiled this way, its controller creates a 2/2 green
 * Boar creature token.
 *
 * "X target creatures" is exactly X (`targets(…, exactly = X)`, as Word of Binding). The per-target
 * body is Hour of Need's: each still-legal target (CR 608.2b) is exiled with a tracked move, and the
 * Boar is created only if that move actually happened ("exiled this way"). The token goes to the
 * creature's controller as it last existed on the battlefield — [EffectTarget.TargetController]
 * reads the re-scoped per-target context and falls back to last-known information.
 */
val CurseOfTheSwine = card("Curse of the Swine") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exile X target creatures. For each creature exiled this way, its controller " +
        "creates a 2/2 green Boar creature token."

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val creature = gather(CardSource.Entity(EffectTarget.ContextTarget(0)))
                    moveTracked(creature, CardDestination.ToZone(Zone.EXILE), name = "exiled")
                },
                then = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Boar"),
                    controller = EffectTarget.TargetController,
                    imageUri = "https://cards.scryfall.io/normal/front/2/f/2f40613b-1bde-4939-86ad-6bd40f9db0d6.jpg?1783939698"
                ),
                successCriterion = SuccessCriterion.CollectionNonEmpty("exiled")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "James Ryman"
        flavorText = "Another imminent battle subsided in busy snuffling and carefree rooting."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78831fc6-ea90-4546-b46a-9c192dbefc65.jpg?1783939800"
    }
}
