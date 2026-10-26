package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Loki, God of Mischief — Marvel Super Heroes #65 (rare)
 * {1}{U} · Legendary Creature — God Sorcerer Villain · 2/1
 *
 * Whenever a player or permanent becomes the target of an ability you control, draw a card.
 * This ability triggers only once each turn.
 *
 * Three separate narrowings of one trigger, all of them data on
 * [com.wingedsheep.sdk.scripting.EventPattern.BecomesTargetEvent]:
 *
 *  - **"an ability"** (not a spell) is `abilitiesOnly`, the mirror of the `spellsOnly` that King of
 *    the Oathbreakers already used. Both activated and triggered abilities count — the engine stamps
 *    `sourceIsSpell` on the event from whichever `StackResolver` entry point put the object on the
 *    stack, and only the cast/spell-copy paths set it.
 *  - **"a player or permanent"** is `players = Player.Any`. Player targets are opt-in on the trigger
 *    side because every other becomes-target wording in the pool is about objects; without the flag
 *    a targeted player matches nothing (see `TriggerMatcher.matchesBecomesTargetTrigger`).
 *  - **"you control"** is `byYou`, compared against the ability's controller, so an opponent
 *    targeting their own creature with their own ability does nothing here.
 *
 * "This ability triggers only once each turn" is `oncePerTurn` on the triggered ability: the cap is
 * spent by the first trigger, so a second targeting ability in the same turn never puts an ability on
 * the stack at all (CR 603.2 — the ability triggers when the event occurs, and this rider stops it
 * from triggering again).
 *
 * The trigger fires at target *announcement* (CR 601.2c for spells; CR 602.2b / 603.3d for activated
 * and triggered abilities), not on resolution — so an ability whose target becomes illegal, or which
 * gets countered, still draws the card. Targeting is also literal (CR 115.10a): an ability that
 * merely affects a player without the word "target" does not trigger Loki.
 */
val LokiGodOfMischief = card("Loki, God of Mischief") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — God Sorcerer Villain"
    power = 2
    toughness = 1
    oracleText = "Whenever a player or permanent becomes the target of an ability you control, " +
        "draw a card. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.a().becomesTarget(byYou = true, abilitiesOnly = true, players = Player.Any)
        oncePerTurn = true
        effect = Effects.DrawCards(1)
        description = "Whenever a player or permanent becomes the target of an ability you " +
            "control, draw a card. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Vilhelmas Banys"
        flavorText = "\"Foolish heroes. Loki can match you trick for trick, and *then* some.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/236c437f-ee9d-4145-a4db-b665908089cf.jpg?1783902955"
    }
}
