package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Intrepid Trufflesnout // Go Hog Wild
 * {1}{G}
 * Creature — Boar
 * 3/1
 * Whenever this creature attacks alone, create a Food token.
 *
 * Adventure: Go Hog Wild — {1}{G}, Instant — Adventure
 * Target creature gets +2/+2 until end of turn.
 *
 * "Attacks alone" is [AttackPredicate.Alone] on the SELF-bound attack trigger — it fires only when
 * the Boar is the *only* creature declared as an attacker (CR 506.5 / the standard "attacks alone"
 * templating), not merely when it is unblocked.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val IntrepidTrufflesnout = card("Intrepid Trufflesnout") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Boar"
    oracleText = "Whenever this creature attacks alone, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.attacks(requires = setOf(AttackPredicate.Alone))
        effect = Effects.CreateFood()
    }

    adventure("Go Hog Wild") {
        manaCost = "{1}{G}"
        typeLine = "Instant — Adventure"
        oracleText = "Target creature gets +2/+2 until end of turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.Creature)
            effect = Effects.ModifyStats(2, 2, t)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "320"
        artist = "Kisung Koh"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4224747e-1dbc-4a29-b5da-5916d8ca2768.jpg?1783915038"
    }
}
