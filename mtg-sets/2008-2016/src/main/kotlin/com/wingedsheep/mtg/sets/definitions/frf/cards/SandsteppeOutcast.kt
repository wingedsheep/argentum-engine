package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sandsteppe Outcast
 * {2}{W}
 * Creature — Human Warrior
 * 2/1
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Create a 1/1 white Spirit creature token with flying.
 */
val SandsteppeOutcast = card("Sandsteppe Outcast") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Create a 1/1 white Spirit creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Spirit"),
                    keywords = setOf(Keyword.FLYING),
                ),
                "Create a 1/1 white Spirit creature token with flying",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f29943bb-aa89-443b-96b7-8f6f24672a9d.jpg?1783938709"
    }
}
