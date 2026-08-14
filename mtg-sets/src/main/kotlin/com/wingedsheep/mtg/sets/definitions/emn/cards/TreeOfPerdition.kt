package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreatureStat
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tree of Perdition
 * {3}{B}
 * Creature — Plant
 * 0/13
 *
 * Defender
 * {T}: Exchange target opponent's life total with this creature's toughness.
 *
 * The exchange is one [Effects.ExchangeLifeAndStat] with `stat = TOUGHNESS` and the life side
 * pointed at the targeted opponent instead of the controller (CR 701.12g). The opponent receives
 * the Tree's *projected* toughness while the Tree's **base** toughness is set at Layer 7b, which
 * is what makes the Cultist's Staff ruling come out right: a 2/15 Tree exchanging against a
 * player on 7 life ends up a 2/9, and that player ends up on 15.
 *
 * If the Tree has left the battlefield by the time the ability resolves the exchange doesn't
 * happen at all — giving it -13/-13 in response is a clean answer, not a way to kill the opponent.
 */
val TreeOfPerdition = card("Tree of Perdition") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Plant"
    power = 0
    toughness = 13
    oracleText = "Defender\n" +
        "{T}: Exchange target opponent's life total with this creature's toughness."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        target = Targets.Opponent
        effect = Effects.ExchangeLifeAndStat(
            target = EffectTarget.Self,
            stat = CreatureStat.TOUGHNESS,
            player = EffectTarget.ContextTarget(0)
        )
        description = "Exchange target opponent's life total with this creature's toughness"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "109"
        artist = "Jung Park"
        flavorText = "There will be no absolution."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/305bd19a-ae5e-46ca-8ff7-27810c968315.jpg?1783937474"
        ruling(
            "2016-07-13",
            "When the ability resolves, Tree of Perdition's toughness becomes the targeted " +
                "opponent's former life total and that player gains or loses an amount of life " +
                "necessary so that their life total equals Tree of Perdition's former toughness."
        )
        ruling(
            "2016-07-13",
            "Any toughness-modifying effects, counters, Auras, or Equipment will apply after its " +
                "toughness is set to the player's former life total."
        )
        ruling(
            "2016-07-13",
            "If Tree of Perdition isn't on the battlefield when the ability resolves, the " +
                "exchange can't happen and the ability will have no effect."
        )
    }
}
