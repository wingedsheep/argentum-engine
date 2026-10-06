package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Goblin Researcher
 * {3}{R}
 * Creature — Goblin Wizard
 * 3/3
 *
 * When this creature enters, exile the top card of your library. During any turn you attacked
 * with this creature, you may play that card.
 *
 * The permission lasts for as long as the card stays exiled ([MayPlayExpiry.Permanent]) and is
 * gated on [Conditions.SourceAttackedThisTurn], re-evaluated on every play query against the
 * granting Researcher (the permission carries its source id). That predicate reads the per-turn
 * attacker record, so it stays true after the Researcher leaves combat or the battlefield — as the
 * rulings require — and a second Researcher's attack never opens this one's card.
 */
val GoblinResearcher = card("Goblin Researcher") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, exile the top card of your library. During any turn " +
        "you attacked with this creature, you may play that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1))
            exile(exiled)
            run(
                Effects.GrantMayPlayFromExile(
                    from = exiled,
                    expiry = MayPlayExpiry.Permanent,
                    condition = Conditions.SourceAttackedThisTurn,
                )
            )
        }
        description = "When this creature enters, exile the top card of your library. During any " +
            "turn you attacked with this creature, you may play that card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Izzy"
        flavorText = "Slonk was struck by lightning twenty-seven times before he got the idea. " +
            "Another forty-three hits and he'd perfected it."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/529e62a1-a32f-477a-ae5c-955f3df2a628.jpg?1783919182"
        ruling("2022-12-02", "The card Goblin Researcher exiles is exiled face up.")
        ruling(
            "2022-12-02",
            "You can play the exiled card if Goblin Researcher attacked and is still in combat, has " +
                "left combat, has left the battlefield, or even if combat is over."
        )
        ruling(
            "2022-12-02",
            "If you control more than one card named Goblin Researcher, each one's effect is " +
                "independent from the other. Specifically, attacking with one will not allow you to " +
                "play the card exiled by the other."
        )
        ruling(
            "2022-12-02",
            "Goblin Researcher's effect doesn't change when you can play the exiled card. For " +
                "example, if you exile a sorcery card, you can cast it only during your main phase " +
                "when the stack is empty."
        )
    }
}
