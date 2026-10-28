package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ironwill Forger — Tarkir: Dragonstorm Commander #13
 * {3}{W} · Creature — Orc Artificer · 3/3
 *
 * Lieutenant — At the beginning of combat on your turn, if you control your commander, target
 * nonlegendary creature you control gains myriad until end of turn.
 *
 * The lieutenant intervening-if is Siege-Gang Lieutenant's ("your commander" = a commander you
 * own). Granting the bare keyword hands the creature the engine's myriad trigger for the turn
 * ([com.wingedsheep.sdk.scripting.Myriad]); the copies it makes don't have it.
 */
val IronwillForger = card("Ironwill Forger") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Orc Artificer"
    power = 3
    toughness = 3
    oracleText = "Lieutenant — At the beginning of combat on your turn, if you control your commander, " +
        "target nonlegendary creature you control gains myriad until end of turn. (Whenever it attacks, " +
        "for each opponent other than defending player, you may create a token copy that's tapped and " +
        "attacking that player or a planeswalker they control. Exile the tokens at end of combat.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(GameObjectFilter.Permanent.commander().ownedByYou())
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl().nonlegendary()))
        effect = Effects.GrantKeyword(Keyword.MYRIAD, creature)
        description = "Lieutenant — At the beginning of combat on your turn, if you control your " +
            "commander, target nonlegendary creature you control gains myriad until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53398f76-7c8a-4009-8f28-9ce586e2c7e4.jpg?1783907184"
        ruling("2025-04-04", "If the defending player is your only opponent, no tokens are put onto the battlefield.")
        ruling("2025-04-04", "You choose whether each token is attacking the player or a planeswalker they control as the token is created. If it's attacking a planeswalker, you choose which one. You can't have any of the tokens attack a battle.")
        ruling("2025-04-04", "Each token copies exactly what was printed on the original creature and nothing else. It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
    }
}
