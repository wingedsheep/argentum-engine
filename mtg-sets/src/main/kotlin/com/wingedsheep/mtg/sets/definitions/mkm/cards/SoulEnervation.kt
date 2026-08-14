package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soul Enervation — Murders at Karlov Manor #106
 * {3}{B} · Enchantment
 *
 * Flash
 * When this enchantment enters, target creature gets -4/-4 until end of turn.
 * Whenever one or more creature cards leave your graveyard, each opponent loses 1 life and you
 * gain 1 life.
 *
 * Flash plus a -4/-4 makes this a combat trick that stays on the battlefield; the second ability
 * is the payoff for the graveyard decks that want the first one to be filling their yard.
 *
 * "One or more creature cards leave your graveyard" is a **batching** trigger (CR 603.2c) —
 * [Triggers.CardsLeaveYourGraveyard] fires at most once per batch no matter how many cards left,
 * which is the printed ruling. Leaving covers every exit: cast from the yard, reanimated, exiled
 * to delve or escape, shuffled back in. The graveyard is scoped to the enchantment's controller,
 * so an opponent recurring their own creature never drains them.
 *
 * The drain is a fixed 1 life gained, not "equal to the life lost" — with three opponents this
 * costs them 1 each and still gains only 1, so it is `LoseLife` + `GainLife` rather than
 * [Effects.DrainLife], whose gain totals across the losers.
 */
val SoulEnervation = card("Soul Enervation") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, target creature gets -4/-4 until end of turn.\n" +
        "Whenever one or more creature cards leave your graveyard, each opponent loses 1 life and " +
        "you gain 1 life."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-4, -4, creature)
        description = "When this enchantment enters, target creature gets -4/-4 until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard(GameObjectFilter.Creature)
        effect = Effects.Composite(
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(1),
        )
        description = "Whenever one or more creature cards leave your graveyard, each opponent " +
            "loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Domenico Cava"
        flavorText = "Judith's plan was close to fruition. She couldn't have Agrus get in the way."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f22ac67-06ce-47cc-a515-d216d30b9cae.jpg?1783912889"

        ruling(
            "2024-02-02",
            "If multiple creature cards leave your graveyard at the same time, Soul Enervation's " +
                "last ability will trigger only once."
        )
    }
}
