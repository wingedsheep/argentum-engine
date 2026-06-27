package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lita, Little Orphan Amphibian
 * {1}{W}
 * Legendary Creature — Mutant Ninja Turtle
 * 2/1
 *
 * Alliance — Whenever another creature you control enters, choose one that hasn't
 * been chosen this turn.
 * • Put a +1/+1 counter on Lita.
 * • Create a Food token.
 * • Scry 1.
 */
val LitaLittleOrphanAmphibian = card("Lita, Little Orphan Amphibian") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Alliance — Whenever another creature you control enters, choose one that hasn't been chosen this turn.\n• Put a +1/+1 counter on Lita.\n• Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n• Scry 1."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = ModalEffect.chooseOneNotYetChosen(
            Mode.noTarget(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)),
            Mode.noTarget(Effects.CreateFood()),
            Mode.noTarget(Effects.Scry(1))
        )
        description = "Alliance — Whenever another creature you control enters, choose one that hasn't been chosen this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Anna Pavleeva"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fbaabb5-e981-4cbf-888c-46449412711f.jpg?1771424706"
    }
}
