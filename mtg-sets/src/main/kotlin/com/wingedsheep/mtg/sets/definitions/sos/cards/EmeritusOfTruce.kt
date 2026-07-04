package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emeritus of Truce // Swords to Plowshares — Secrets of Strixhaven #13
 * {1}{W}{W} · Creature — Cat Cleric · 3/3
 *
 * When this creature enters, target player creates a 1/1 white and black Inkling creature token
 * with flying. Then if an opponent controls more creatures than you, this creature becomes
 * prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)
 * //
 * Swords to Plowshares — {W}, Instant: Exile target creature. Its controller gains life equal to
 * its power.
 *
 * Prepare (Secrets of Strixhaven): the "becomes prepared" clause is a resolution-time
 * conditional (CR 603.4 "Then if …") sequenced AFTER the token creation, not a separate trigger
 * or intervening-if. Becoming prepared (via [Effects.BecomePrepared]) creates a copy of its
 * prepare spell ("Swords to Plowshares") in exile that its controller may cast for {W}; casting
 * that copy unprepares the creature. Modeled via [com.wingedsheep.sdk.model.CardLayout.PREPARE]
 * + the `prepare(name) { }` DSL.
 */
val EmeritusOfTruce = card("Emeritus of Truce") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, target player creates a 1/1 white and black Inkling " +
        "creature token with flying. Then if an opponent controls more creatures than you, this " +
        "creature becomes prepared. (While it's prepared, you may cast a copy of its spell. Doing " +
        "so unprepares it.)"

    keywords(Keyword.PREPARED)

    // When this creature enters, target player creates a 1/1 W/B Inkling with flying.
    // Then if an opponent controls more creatures than you, it becomes prepared.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val recipient = target("target player", Targets.Player)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Inkling"),
            keywords = setOf(Keyword.FLYING),
            controller = recipient,
            imageUri = "https://cards.scryfall.io/display/front/b/a/bab52920-9d67-4cd4-9015-6e645ff9764f.webp?1782723480"
        ) then ConditionalEffect(
            condition = Conditions.OpponentControlsMoreCreatures,
            effect = Effects.BecomePrepared(EffectTarget.Self),
        )
    }

    // Swords to Plowshares — the prepare spell. Exile target creature; its controller gains
    // life equal to its power. Gain life FIRST so it reads the creature's power while it is
    // still on the battlefield (there is no last-known-power fallback for a spell target).
    prepare("Swords to Plowshares") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Exile target creature. Its controller gains life equal to its power."
        spell {
            val creature = target("target creature", Targets.Creature)
            effect = Effects.GainLife(
                com.wingedsheep.sdk.dsl.DynamicAmounts.targetPower(0),
                EffectTarget.TargetController,
            ) then Effects.Exile(creature)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9869a753-5e41-4098-ab41-e75b4396ec50.jpg?1778165061"
    }
}
