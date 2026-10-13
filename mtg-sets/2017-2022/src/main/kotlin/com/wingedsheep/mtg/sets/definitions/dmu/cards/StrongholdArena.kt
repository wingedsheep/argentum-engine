package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stronghold Arena
 * {1}{B}
 * Enchantment
 * Kicker {G} and/or {W}
 * When this enchantment enters, you gain 3 life for each time it was kicked.
 * Whenever one or more creatures you control deal combat damage to a player, you may reveal the
 * top card of your library and put it into your hand. If you do, you lose life equal to its mana
 * value.
 *
 * - "Kicker {G} and/or {W}" is two kicker abilities in printed order (Urborg Lhurgoyf's shape), so
 *   "each time it was kicked" is 0, 1 or 2 — summed from the two linked kicker slots into one
 *   life gain of 3 × kicks rather than one gain per kicker.
 * - The combat trigger batches per damaged player (it fires once per opponent hit, per combat
 *   damage step — the ruling). Unlike Sorin, the Mirthless you don't look first (ruling: you decide
 *   before seeing the card), so the whole reveal-and-take sits inside the "may"; an empty library
 *   reveals nothing and costs no life.
 */
val StrongholdArena = card("Stronghold Arena") {
    manaCost = "{1}{B}"
    colorIdentity = "WBG"
    typeLine = "Enchantment"
    oracleText = "Kicker {G} and/or {W} (You may pay an additional {G} and/or {W} as you cast this spell.)\n" +
        "When this enchantment enters, you gain 3 life for each time it was kicked.\n" +
        "Whenever one or more creatures you control deal combat damage to a player, you may reveal the " +
        "top card of your library and put it into your hand. If you do, you lose life equal to its mana value."

    keywordAbility(KeywordAbility.kicker("{G}"))
    keywordAbility(KeywordAbility.kicker("{W}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(
            DynamicAmounts.conditional(Conditions.WasKickedWithFirstKicker, 3, 0) +
                DynamicAmounts.conditional(Conditions.WasKickedWithSecondKicker, 3, 0)
        )
        description = "When this enchantment enters, you gain 3 life for each time it was kicked."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.May(
            Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(1))
                toHand(top, revealed = true)
                run(Effects.LoseLife(DynamicAmounts.manaValueOf(top), EffectTarget.Controller))
            },
            prompt = "Reveal the top card of your library and put it into your hand? " +
                "You lose life equal to its mana value."
        )
        description = "Whenever one or more creatures you control deal combat damage to a player, you may " +
            "reveal the top card of your library and put it into your hand. If you do, you lose life equal " +
            "to its mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb64b0a7-3e9a-490f-bf02-c62fc8cf750d.jpg?1783921324"

        ruling(
            "2022-09-09",
            "Normally, combat damage is dealt all at the same time. In that case, Stronghold Arena's last ability triggers once for each opponent that creatures you control dealt combat damage to, regardless of how many creatures were dealing that damage. If any of those creatures have double strike or first strike, the ability will trigger once for each opponent dealt damage in each combat damage step."
        )
        ruling(
            "2022-09-09",
            "As Stronghold Arena's last ability resolves, you don't get to see what the card of your library is before deciding whether to reveal it. If you choose to reveal it, you must put it into your hand and will lose life."
        )
        ruling(
            "2022-09-09",
            "You may kick Stronghold Arena only once for its {G} cost and only once for its {W} cost. You can kick it twice by paying {G}{W}, but you can't kick it twice by paying {G}{G} or {W}{W}. You can't kick it more than twice."
        )
    }
}
