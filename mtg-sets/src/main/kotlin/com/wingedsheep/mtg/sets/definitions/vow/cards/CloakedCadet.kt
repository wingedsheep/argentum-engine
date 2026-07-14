package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cloaked Cadet
 * {4}{G}
 * Creature — Human Ranger
 * 2/4
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * Whenever one or more +1/+1 counters are put on one or more Humans you control, draw a card.
 * This ability triggers only once each turn.
 *
 * Two independent pieces:
 *  - [training] gives the keyword + the attack trigger. Cadet is itself a Human, so the very
 *    +1/+1 counter Training places on it satisfies the draw watcher below.
 *  - A hand-written [Triggers.countersPlacedOn] watcher over "+1/+1 counters on Humans you
 *    control". `firstTimeEachTurn = false` because the CR wording caps the *ability* at once per
 *    turn (`oncePerTurn = true`), not per-permanent — the once-per-turn gate belongs on the
 *    ability, and per-permanent first-time tracking would wrongly re-arm for a second Human.
 *    Per Scryfall rulings you draw only one card no matter how many counters land or on how many
 *    Humans, which `oncePerTurn = true` delivers (the trigger fires once and won't re-trigger).
 */
val CloakedCadet = card("Cloaked Cadet") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ranger"
    power = 2
    toughness = 4
    oracleText = "Training (Whenever this creature attacks with another creature with greater " +
        "power, put a +1/+1 counter on this creature.)\n" +
        "Whenever one or more +1/+1 counters are put on one or more Humans you control, draw a " +
        "card. This ability triggers only once each turn."

    training()

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl(),
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            firstTimeEachTurn = false,
        )
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06d86c79-d2c7-4900-9474-4d4bc4c74d44.jpg?1783924816"
    }
}
