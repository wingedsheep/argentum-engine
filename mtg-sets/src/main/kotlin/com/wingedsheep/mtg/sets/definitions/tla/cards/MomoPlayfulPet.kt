package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Momo, Playful Pet
 * {W}
 * Legendary Creature — Lemur Bat Ally
 * 1/1
 *
 * Flying, vigilance
 * When Momo leaves the battlefield, choose one —
 * • Create a Food token.
 * • Put a +1/+1 counter on target creature you control.
 * • Scry 2.
 *
 * A modal leaves-battlefield trigger (`ModalEffect.chooseOne`). Only the second mode targets, so
 * it carries its own [Targets.CreatureYouControl] requirement via [Mode.withTarget]; the Food and
 * Scry modes are [Mode.noTarget].
 */
val MomoPlayfulPet = card("Momo, Playful Pet") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Lemur Bat Ally"
    power = 1
    toughness = 1
    oracleText = "Flying, vigilance\n" +
        "When Momo leaves the battlefield, choose one —\n" +
        "• Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "• Put a +1/+1 counter on target creature you control.\n" +
        "• Scry 2."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = ModalEffect.chooseOne(
            Mode.noTarget(Effects.CreateFood(), "Create a Food token."),
            Mode.withTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                Targets.CreatureYouControl,
                "Put a +1/+1 counter on target creature you control."
            ),
            Mode.noTarget(Effects.Scry(2), "Scry 2.")
        )
        description = "When Momo leaves the battlefield, choose one — Create a Food token; " +
            "put a +1/+1 counter on target creature you control; or Scry 2."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Awanqi (Angela Wang)"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9350bf4a-fa12-4867-b31f-1f1394d99571.jpg?1764120089"
    }
}
