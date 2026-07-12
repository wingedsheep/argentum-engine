package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wedding Security
 * {3}{B}{B}
 * Creature — Vampire Soldier
 * 4/4
 *
 * Whenever this creature attacks, you may sacrifice a Blood token. If you do, put a +1/+1 counter
 * on this creature and draw a card.
 *
 * Attack trigger with an optional Blood-sacrifice gate ([Gate.MayPay] over a Blood
 * [SacrificeEffect], the Bloodcrazed Socialite idiom); paying puts a +1/+1 counter on the attacker
 * and draws a card.
 */
val WeddingSecurity = card("Wedding Security") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature attacks, you may sacrifice a Blood token. If you do, put " +
        "a +1/+1 counter on this creature and draw a card."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = GatedEffect(
            gate = Gate.MayPay(SacrificeEffect(filter = GameObjectFilter.Artifact.withSubtype("Blood"))),
            then = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                .then(Effects.DrawCards(1))
        )
        description = "Whenever this creature attacks, you may sacrifice a Blood token. If you do, " +
            "put a +1/+1 counter on this creature and draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "299"
        artist = "Vi Szendrey (Cashile)"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20b1a617-96bf-4960-bffe-dddf3a3c1868.jpg?1782702985"
    }
}
