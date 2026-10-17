package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Entangling Trap
 * {1}{W}
 * Enchantment
 * Whenever you clash, tap target creature an opponent controls. If you won, that creature doesn't
 * untap during its controller's next untap step. (This ability triggers after the clash ends.)
 *
 * The clash *payoff* that acts either way — the sibling of Sylvan Echoes, which only acts on a win.
 * The tap is unconditional, so the trigger is `Triggers.you.clashes()` and the win rides inside
 * the effect as [Conditions.YouWonTheClash] rather than on the trigger: losing still puts the
 * ability on the stack and still taps a creature. Per its ruling that also holds when an opponent's
 * spell started the clash — you clashed, so it triggers, and you can still have won.
 *
 * Both halves address the same target slot, which is what makes the printed
 * "that creature" one creature rather than two: a second trigger gated on winning would re-target.
 * The freeze is the Crippling Chill shape — [AbilityFlag.DOESNT_UNTAP] for
 * [Duration.UntilAfterAffectedControllersNextUntap], which expires with that one untap step rather
 * than at end of turn.
 */
val EntanglingTrap = card("Entangling Trap") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever you clash, tap target creature an opponent controls. If you won, that " +
        "creature doesn't untap during its controller's next untap step. " +
        "(This ability triggers after the clash ends.)"

    triggeredAbility {
        trigger = Triggers.you.clashes()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.If(
                Conditions.YouWonTheClash,
                Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    creature,
                    Duration.UntilAfterAffectedControllersNextUntap()
                )
            )
        description = "tap target creature an opponent controls. If you won, that creature " +
            "doesn't untap during its controller's next untap step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d1cec6f-5308-4bff-b19a-df9e3325bb06.jpg?1783942915"
    }
}
