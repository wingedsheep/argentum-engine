package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kitsune Ace — Kamigawa: Neon Dynasty #22 (canonical printing)
 * {1}{W} · Creature — Fox Pilot · 2/2
 *
 * Whenever a Vehicle you control attacks, choose one —
 * • That Vehicle gains first strike until end of turn.
 * • Untap this creature.
 *
 * The per-attacker `Triggers.a(filter).attacks()` shape (as on Caught in the Brights), so each
 * attacking Vehicle triggers separately and "that Vehicle" is [EffectTarget.TriggeringEntity].
 * Neither mode targets; the mode is chosen as the trigger is put on the stack (the Retreat to
 * Valakut idiom). The untap mode is the Pilot's payoff: tap it to crew, then untap it when the
 * Vehicle attacks so it can block on the opponent's turn.
 */
val KitsuneAce = card("Kitsune Ace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Pilot"
    power = 2
    toughness = 2
    oracleText = "Whenever a Vehicle you control attacks, choose one —\n" +
        "• That Vehicle gains first strike until end of turn.\n" +
        "• Untap this creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl()).attacks()
        effect = ModalEffect.chooseOne(
            mode("That Vehicle gains first strike until end of turn") {
                effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.TriggeringEntity)
            },
            mode("Untap this creature") {
                effect = Effects.Untap(EffectTarget.Self)
            },
        )
        description = "Whenever a Vehicle you control attacks, choose one — that Vehicle gains " +
            "first strike until end of turn; or untap this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Joseph Weston"
        flavorText = "\"Oh, sorry—were we racing?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef1d446a-8607-45b8-a01f-dad17bab76e7.jpg?1783923919"
    }
}
