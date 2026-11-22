package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenacious Dead (Magic 2014 #118)
 * {B} · Creature — Skeleton Warrior · 1/1
 *
 * When this creature dies, you may pay {1}{B}. If you do, return it to the battlefield tapped under
 * its owner's control.
 *
 * "You may pay … If you do" is [Effects.MayPay]: the trigger's controller decides on resolution, and
 * the return happens only if the mana is paid. The move names `Self` with `fromZone = GRAVEYARD`, so a
 * card that left the graveyard in response stays where it is; a card put onto the battlefield from
 * the graveyard enters under its owner's control, matching the second ruling.
 */
val TenaciousDead = card("Tenacious Dead") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Warrior"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, you may pay {1}{B}. If you do, return it to the battlefield " +
        "tapped under its owner's control."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}{B}"),
            then = Effects.Move(
                EffectTarget.Self,
                Zone.BATTLEFIELD,
                ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD,
            ),
        )
        description = "When this creature dies, you may pay {1}{B}. If you do, return it to the " +
            "battlefield tapped under its owner's control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "John Stanko"
        flavorText = "Raising the bones of Hekjek the Mad proved far easier than getting them to lie back down."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b96fed2-0be9-4181-94ae-10f031e2aeb2.jpg?1783939918"
        ruling(
            "2013-07-01",
            "You decide whether to pay {1}{B} when the ability resolves. If you choose not to, you won't " +
                "get another chance later."
        )
        ruling(
            "2013-07-01",
            "If you control a Tenacious Dead owned by another player when it dies, you'll control the " +
                "triggered ability. However, if you choose to pay {1}{B}, it will be put onto the " +
                "battlefield under its owner's control."
        )
    }
}
