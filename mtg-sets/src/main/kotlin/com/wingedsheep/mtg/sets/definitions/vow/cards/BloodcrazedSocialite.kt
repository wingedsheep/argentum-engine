package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
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
 * Bloodcrazed Socialite
 * {3}{B}
 * Creature — Vampire
 * 3/3
 *
 * Menace
 * When this creature enters, create a Blood token.
 * Whenever this creature attacks, you may sacrifice a Blood token. If you do, it gets +2/+2 until
 * end of turn.
 */
val BloodcrazedSocialite = card("Bloodcrazed Socialite") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "Menace\n" +
        "When this creature enters, create a Blood token. (It's an artifact with \"{1}, {T}, " +
        "Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "Whenever this creature attacks, you may sacrifice a Blood token. If you do, it gets +2/+2 " +
        "until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateBlood(1)
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = GatedEffect(
            gate = Gate.MayPay(SacrificeEffect(filter = GameObjectFilter.Artifact.withSubtype("Blood"))),
            then = Effects.ModifyStats(2, 2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "288"
        artist = "Samuel Araya"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9df22639-7e98-43a1-801e-cbf882558c53.jpg?1782702993"
    }
}
