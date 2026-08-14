package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode

/**
 * Breeches, Eager Pillager
 * {2}{R}
 * Legendary Creature — Goblin Pirate
 * 3/3
 *
 * First strike
 * Whenever a Pirate you control attacks, choose one that hasn't been chosen this turn —
 * • Create a Treasure token.
 * • Target creature can't block this turn.
 * • Exile the top card of your library. You may play it this turn.
 *
 * Implementation notes:
 * - The trigger fires once for each attacking Pirate you control ([TriggerBinding.ANY]), including
 *   Breeches itself when it attacks (it is a Pirate).
 * - "Choose one that hasn't been chosen this turn" is [ModalEffect.chooseOneNotYetChosenThisTurn]:
 *   the engine remembers which of the three modes this Breeches has chosen during the current turn
 *   (per-source [com.wingedsheep.engine.state.components.battlefield.ChosenModesThisTurnComponent],
 *   cleared each cleanup step) and never offers an already-chosen mode again until end of turn.
 *   Across all of a turn's Pirate-attack triggers each mode is chosen at most once; once all three
 *   have been chosen this turn the ability has no legal mode and resolves as a no-op (per the
 *   card's ruling: "removed from the stack with no effect"). The memory resets next turn.
 * - Per the card's ruling, the memory is keyed to the source object, so two Breeches you control
 *   track their chosen modes separately.
 * - Mode B ("target creature can't block") targets any creature (CR 603.3d — the target is chosen
 *   as the trigger goes on the stack); [Effects.CantBlock] defaults to [com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget]
 *   0 with [com.wingedsheep.sdk.core.Duration.EndOfTurn].
 * - Mode C is an impulse draw ([Patterns.Exile.impulse]): exile the top card into a per-instance
 *   collection and grant a may-play-from-exile permission that expires at end of turn; the played
 *   card still pays its costs and follows normal timing (a land only during your main phase, etc.).
 */
val BreechesEagerPillager = card("Breeches, Eager Pillager") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Pirate"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "Whenever a Pirate you control attacks, choose one that hasn't been chosen this turn —\n" +
        "• Create a Treasure token.\n" +
        "• Target creature can't block this turn.\n" +
        "• Exile the top card of your library. You may play it this turn."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.PIRATE).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            // • Create a Treasure token.
            Mode.noTarget(
                Effects.CreateTreasure(1),
                "Create a Treasure token",
            ),
            // • Target creature can't block this turn.
            Mode.withTarget(
                Effects.CantBlock(),
                Targets.Creature,
                "Target creature can't block this turn",
            ),
            // • Exile the top card of your library. You may play it this turn.
            Mode.noTarget(
                Patterns.Exile.impulse(count = 1, storeAs = "breechesExiled"),
                "Exile the top card of your library. You may play it this turn",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aadf5028-8dfe-40d3-89b4-22bd7ed0aae6.jpg?1782694500"

        ruling("2023-11-10", "You pay all costs and follow all normal timing rules for cards played this way. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2023-11-10", "If you can't legally choose a mode because all three have been chosen that turn, that instance of the ability is removed from the stack with no effect.")
        ruling("2023-11-10", "If you somehow control two or more Breeches, Eager Pillager, track which modes have been chosen each turn for each one's ability separately.")
    }
}
