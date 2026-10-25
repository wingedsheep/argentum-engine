package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Maddening Cacophony
 * {1}{U}
 * Sorcery
 * Kicker {3}{U}
 * Each opponent mills eight cards. If this spell was kicked, instead each opponent mills half their
 * library, rounded up.
 *
 * [WasKicked] picks between the two mills. The kicked half is per player — "half *their* library" —
 * so it iterates [Player.EachOpponent] with [Effects.ForEachPlayer], which rebinds [Player.You] to
 * the iterated opponent; each one's count is read off their own library and rounded up with
 * [divRoundedUp] (Pox Plague's shape).
 */
val MaddeningCacophony = card("Maddening Cacophony") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Kicker {3}{U}\n" +
        "Each opponent mills eight cards. If this spell was kicked, instead each opponent mills half their library, rounded up."

    keywordAbility(KeywordAbility.kicker("{3}{U}"))

    spell {
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.ForEachPlayer(
                players = Player.EachOpponent,
                effect = Patterns.Library.mill(
                    DynamicAmounts.zone(Player.You, Zone.LIBRARY).count() divRoundedUp 2
                )
            ),
            otherwise = Patterns.Library.mill(8, EffectTarget.PlayerRef(Player.EachOpponent))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Magali Villeneuve"
        flavorText = "Jace traced Nahiri to the Singing City, but the magic of the ancient ruins threatened to overwhelm his mind."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10a79733-702c-4611-b073-71db7f1158b2.jpg?1783929393"
    }
}
