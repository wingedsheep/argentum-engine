package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Persuasive Interrogators — Murders at Karlov Manor #98
 * {4}{B}{B} · Creature — Gorgon Detective · 5/6
 *
 * When this creature enters, investigate.
 * Whenever you sacrifice a Clue, target opponent gets two poison counters.
 *
 * The infect payoff in a set with no other poison: every Clue you cash in is two counters, so
 * five Clues is a kill on their own.
 *
 * [Triggers.YouSacrificeA] is the per-permanent form — "whenever you sacrifice **a** Clue"
 * triggers once per Clue (CR 603.2), so sacrificing two at once gives four counters across two
 * separate abilities, each with its own target. It fires on *any* sacrifice, including paying a
 * Clue's own "{2}, Sacrifice this token: Draw a card" cost, because costs are paid before the
 * ability goes on the stack and the trigger reads the sacrifice event. Same shape as
 * [CuriousCadaver]'s recursion trigger.
 *
 * Poison counters go on a player, which [Effects.AddCounters] resolves the same way it does for
 * a permanent — the target is a player-shaped `TargetOpponent`, so an opponent who becomes an
 * illegal target (hexproof from a player-targeting effect, or having left a multiplayer game)
 * fizzles the whole ability.
 */
val PersuasiveInterrogators = card("Persuasive Interrogators") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon Detective"
    power = 5
    toughness = 6
    oracleText = "When this creature enters, investigate. (Create a Clue token. It's an artifact " +
        "with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice a Clue, target opponent gets two poison counters. (A player with " +
        "ten or more poison counters loses the game.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Investigate()
        description = "When this creature enters, investigate."
    }

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Clue"))
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.AddCounters(Counters.POISON, 2, opponent)
        description = "Whenever you sacrifice a Clue, target opponent gets two poison counters."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0713025-581f-451b-97a3-97d891285dcc.jpg?1783912892"

        ruling(
            "2024-02-02",
            "Some abilities trigger \"whenever you sacrifice a Clue\". Those abilities trigger " +
                "whenever you sacrifice a Clue for any reason, not just to activate a Clue's " +
                "activated ability."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token."
        )
        ruling(
            "2024-02-02",
            "You can't sacrifice a Clue to pay multiple costs. For example, you can't sacrifice a " +
                "Clue token to activate its own ability and also to activate another ability."
        )
    }
}
