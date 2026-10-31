package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers

/**
 * Doorkeeper Thrull — Murders at Karlov Manor #13
 * {1}{W} · Creature — Thrull · 1/2 · Rare
 *
 * Flash
 * Flying
 * Artifacts and creatures entering don't cause abilities to trigger.
 *
 * Torpor Orb on a flash flier — the two-mana body is almost incidental to the lock, and the flash is
 * the point: hold it up, and the moment an opponent's Sun Titan or Clue-maker resolves, the Thrull
 * lands first and the trigger never happens.
 *
 * Straight [SuppressZoneChangeTriggers], widened from its `GameObjectFilter.Creature` default to
 * [GameObjectFilter.CreatureOrArtifact]. Everything the rulings care about is already the shared
 * static's documented behaviour, so nothing is card-specific here:
 *
 * - **The gate is the entering object, not the watching trigger's text.** Suppression is decided by
 *   matching the entering permanent against the filter in *projected* state, so an artifact land
 *   entering stops a "whenever a land enters" trigger, and an enchantment that Opalescence has made
 *   a creature is suppressed too. Both are printed rulings, and both fall out of reading projected
 *   state rather than the trigger's wording.
 * - **Replacement effects are untouched.** Enters-tapped, enters-with-counters, and "as this enters"
 *   choices run through the replacement layer, which the static never sees.
 * - **Simultaneous entries are covered.** The engine applies this as a final filtering pass over the
 *   whole batch of pending triggers, so the Thrull entering alongside another creature suppresses
 *   both — the ruling's exact case — rather than racing its own arrival.
 *
 * The Thrull is itself a creature, so its own entry causes no triggers either; that is the printed
 * behaviour, not an oversight.
 */
val DoorkeeperThrull = card("Doorkeeper Thrull") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Thrull"
    power = 1
    toughness = 2
    oracleText = "Flash\nFlying\n" +
        "Artifacts and creatures entering don't cause abilities to trigger."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = SuppressZoneChangeTriggers(GameObjectFilter.CreatureOrArtifact)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Camille Alquier"
        flavorText = "Parties at Karlov Manor are strictly invitation only."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80a1cd28-d2a5-4d1a-aa03-a6a5958ae432.jpg?1783912925"

        ruling(
            "2024-02-02",
            "Replacement effects, such as a permanent entering the battlefield tapped or with " +
                "counters on it, are unaffected. Abilities that apply \"as [this permanent] enters " +
                "the battlefield\" are replacement effects."
        )
        ruling(
            "2024-02-02",
            "The trigger event doesn't have to specify artifacts or creatures entering the " +
                "battlefield. For example, Tireless Tracker has an ability that says \"Whenever a " +
                "land enters the battlefield under your control, investigate.\" If an artifact " +
                "land or creature land enters the battlefield under your control, that ability " +
                "won't trigger."
        )
        ruling(
            "2024-02-02",
            "Look at the permanent as it exists on the battlefield, taking into account continuous " +
                "effects, to determine whether any triggered abilities will trigger."
        )
        ruling(
            "2024-02-02",
            "If Doorkeeper Thrull and another creature or an artifact enter the battlefield at the " +
                "same time, neither one will cause triggered abilities to trigger when they enter " +
                "the battlefield."
        )
    }
}
