package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Homicide Investigator — Murders at Karlov Manor #86
 * {1}{B} · Creature — Human Detective · 2/2
 *
 * Whenever one or more nontoken creatures you control die, investigate.
 * This ability triggers only once each turn.
 *
 * Three separate restrictions, each modelled by its own knob rather than approximated:
 *
 * - **"one or more … die"** is a *batched* death trigger, not a per-creature one. A board wipe that
 *   kills four of your creatures fires this once (CR 603.3b). Using the per-creature
 *   [Triggers.YourCreatureDies] would fire four times and over-produce Clues even before the
 *   once-per-turn cap kicked in.
 * - **"nontoken"** filters the batch, not the trigger's aftermath: a batch containing only tokens
 *   never fires at all, while a mixed batch fires once. `GameObjectFilter.Creature.nontoken()`
 *   applies the predicate at batch-membership time, which is exactly that.
 * - **"only once each turn"** is the first-class `oncePerTurn` cap. The cap is spent by the first
 *   *trigger*, not by the first resolution, so a second batch of deaths later in the same turn
 *   simply doesn't trigger — it isn't a "may" the player can decline to save.
 *
 * Homicide Investigator's own death counts: it's a nontoken creature you control, and the batch is
 * evaluated from last-known information, so trading it off in combat still draws you the Clue.
 */
val HomicideInvestigator = card("Homicide Investigator") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Detective"
    oracleText = "Whenever one or more nontoken creatures you control die, investigate. This " +
        "ability triggers only once each turn. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.OneOrMoreCreaturesYouControlDie(GameObjectFilter.Creature.nontoken())
        oncePerTurn = true
        effect = Effects.Investigate()
        description = "Whenever one or more nontoken creatures you control die, investigate. " +
            "This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Jodie Muir"
        flavorText = "In her line of work, the dead are often better sources than the living."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21d6accd-167a-4b21-a488-44d54cdfa608.jpg?1783912896"
    }
}
