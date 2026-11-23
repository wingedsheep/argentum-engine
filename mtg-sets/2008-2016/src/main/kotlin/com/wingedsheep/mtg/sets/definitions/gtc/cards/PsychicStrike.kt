package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psychic Strike — Gatecrash #189
 * {1}{U}{B} · Instant
 *
 * Counter target spell. Its controller mills two cards.
 *
 * Thought Collapse's shape: the mill reads the spell's controller through `Player.ControllerOf`,
 * which still answers once the spell is countered — and when it can't be countered (ruling).
 */
val PsychicStrike = card("Psychic Strike") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Counter target spell. Its controller mills two cards."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then
            Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.ControllerOf("target spell")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Mathias Kollros"
        flavorText = "Until more information could be gleaned about Niv-Mizzet's intentions, Lazav took every opportunity to stall his experiments."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d87927c-80a6-4146-92a5-58c510ce7958.jpg?1783940102"
        ruling(
            "2013-01-24",
            "If the target spell is an illegal target when Psychic Strike tries to resolve, Psychic Strike " +
                "won't resolve and none of its effects will happen. The target's controller won't put any cards " +
                "from the top of their library into their graveyard."
        )
        ruling(
            "2013-01-24",
            "You may target a spell that can't be countered. When Psychic Strike resolves, the target spell " +
                "will be unaffected, but its controller will still put the top two cards of their library into " +
                "their graveyard."
        )
    }
}
