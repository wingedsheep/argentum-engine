package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Strixhaven Stadium — Strixhaven: School of Mages #259 (canonical printing)
 * {3} · Artifact
 *
 * {T}: Add {C}. Put a point counter on this artifact.
 * Whenever a creature deals combat damage to you, remove a point counter from this artifact.
 * Whenever a creature you control deals combat damage to an opponent, put a point counter on this
 * artifact. Then if it has ten or more point counters on it, remove them all and that player
 * loses the game.
 *
 * - The mana ability adds {C} then a [CounterType.POINT] on itself (Cryptex's "add mana, then a
 *   counter" mana ability).
 * - Both combat triggers are per-creature ANY-bound damage observers, so several creatures
 *   connecting trigger the matching ability that many times (ruling).
 * - On the offensive trigger the damaged opponent is `Player.TriggeringPlayer` — "that player"
 *   (Gonti, Night Minister's binding). The ten-counter check is a resolution-time [Effects.If] on
 *   [Conditions.SourceCounterCountAtLeast], so a tenth counter added some other way doesn't end
 *   the game until this ability next resolves (ruling) — Contested Game Ball's shape.
 */
val StrixhavenStadium = card("Strixhaven Stadium") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}. Put a point counter on this artifact.\n" +
        "Whenever a creature deals combat damage to you, remove a point counter from this artifact.\n" +
        "Whenever a creature you control deals combat damage to an opponent, put a point counter " +
        "on this artifact. Then if it has ten or more point counters on it, remove them all and " +
        "that player loses the game."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(1) then
            Effects.AddCounters(CounterType.POINT, 1, EffectTarget.Self)
        description = "{T}: Add {C}. Put a point counter on this artifact."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.You)
        effect = Effects.RemoveCounters(CounterType.POINT, 1, EffectTarget.Self)
        description = "Whenever a creature deals combat damage to you, remove a point counter " +
            "from this artifact."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.Opponent)
        effect = Effects.AddCounters(CounterType.POINT, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.POINT, 10),
                then = Effects.RemoveAllCountersOfType(CounterType.POINT, EffectTarget.Self) then
                    Effects.LoseGame(EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            )
        description = "Whenever a creature you control deals combat damage to an opponent, put a " +
            "point counter on this artifact. Then if it has ten or more point counters on it, " +
            "remove them all and that player loses the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/421674ee-4b85-4942-b166-952598165826.jpg?1783927278"
        ruling("2021-04-16", "“That player” refers to the opponent who was dealt damage.")
        ruling("2021-04-16", "If multiple creatures deal combat damage to you or an opponent at the same time, the appropriate ability of Strixhaven Stadium will trigger that many times. You can put these abilities on the stack in any order. The last ability put on the stack will be the first to resolve.")
        ruling("2021-04-16", "The check for ten or more point counters happens only as the last ability is resolving. Notably, if you add the tenth point counter some other way, no one will lose the game . . . yet.")
    }
}
