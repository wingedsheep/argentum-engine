package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sauron, the Dark Lord — The Lord of the Rings: Tales of Middle-earth #224
 * {3}{U}{B}{R} · Legendary Creature — Avatar Horror · 7/6 · Mythic
 *
 * Ward—Sacrifice a legendary artifact or legendary creature.
 * Whenever an opponent casts a spell, amass Orcs 1.
 * Whenever an Army you control deals combat damage to a player, the Ring tempts you.
 * Whenever the Ring tempts you, you may discard your hand. If you do, draw four cards.
 *
 * All four pieces compose existing primitives:
 *  - Ward—sacrifice via [KeywordAbility.wardSacrifice] over a "legendary artifact or legendary
 *    creature" filter (legendary supertype + (artifact OR creature)).
 *  - Opponent-cast amass via [Triggers.OpponentCastsSpell] + [Effects.Amass].
 *  - Army-damage Ring-tempt via the generic [Triggers.dealsDamage] factory bound ANY with a
 *    source filter of "an Army you control" (Subtype Army, controlled by you), recipient any player.
 *  - Ring-tempt payoff via [Triggers.RingTemptsYou] + the standard [MayEffect]/[IfYouDoEffect]
 *    pair wrapping [Patterns.Hand.discardHand] then drawing four.
 */
val SauronTheDarkLord = card("Sauron, the Dark Lord") {
    manaCost = "{3}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Avatar Horror"
    power = 7
    toughness = 6
    oracleText = "Ward—Sacrifice a legendary artifact or legendary creature.\n" +
        "Whenever an opponent casts a spell, amass Orcs 1.\n" +
        "Whenever an Army you control deals combat damage to a player, the Ring tempts you.\n" +
        "Whenever the Ring tempts you, you may discard your hand. If you do, draw four cards."

    keywordAbility(
        KeywordAbility.wardSacrifice(
            GameObjectFilter.Artifact.legendary() or GameObjectFilter.Creature.legendary()
        )
    )

    // Whenever an opponent casts a spell, amass Orcs 1.
    triggeredAbility {
        trigger = Triggers.OpponentCastsSpell
        effect = Effects.Amass(1, "Orc")
    }

    // Whenever an Army you control deals combat damage to a player, the Ring tempts you.
    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.youControl().withSubtype("Army"),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.TheRingTemptsYou()
    }

    // Whenever the Ring tempts you, you may discard your hand. If you do, draw four cards.
    triggeredAbility {
        trigger = Triggers.RingTemptsYou
        effect = MayEffect(
            IfYouDoEffect(
                action = Patterns.Hand.discardHand(EffectTarget.Controller),
                ifYouDo = Effects.DrawCards(4)
            )
        )
        description = "You may discard your hand. If you do, draw four cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "224"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/034e0929-b2c7-4b5f-94f2-8eaf4fb1a2a1.jpg?1693611218"
    }
}
