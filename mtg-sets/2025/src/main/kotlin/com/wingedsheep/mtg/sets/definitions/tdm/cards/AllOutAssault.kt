package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

/**
 * All-Out Assault — Tarkir: Dragonstorm #167
 * {2}{R}{W}{B} · Enchantment · Mythic
 *
 * Creatures you control get +1/+1 and have deathtouch.
 * When this enchantment enters, if it's your main phase, there is an additional combat phase
 * after this phase followed by an additional main phase. When you next attack this turn,
 * untap each creature you control.
 *
 * The "when you next attack this turn" clause is a one-shot event-based delayed triggered
 * ability (`CreateDelayedTriggerEffect(trigger = Triggers.you.attacks(), fireOnce = true)`): it
 * fires the first time you declare attackers this turn — refreshing your team for the bonus
 * combat — then removes itself, so a second attack the same turn (e.g. in yet another combat)
 * won't untap again. See item 15 of `backlog/archived/tdm-engine-gaps.md`.
 */
val AllOutAssault = card("All-Out Assault") {
    manaCost = "{2}{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1 and have deathtouch.\n" +
        "When this enchantment enters, if it's your main phase, there is an additional combat " +
        "phase after this phase followed by an additional main phase. When you next attack this " +
        "turn, untap each creature you control."

    // Creatures you control get +1/+1 and have deathtouch.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }
    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.AllCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.IsYourMainPhase
        // "there is an additional combat phase after this phase followed by an
        // additional main phase"
        effect = Effects.AddCombatPhase then
            Effects.AddMainPhase then
            // "When you next attack this turn, untap each creature you control."
            Effects.CreateDelayedTrigger(
                trigger = Triggers.you.attacks(),
                fireOnce = true,
                effect = Patterns.Group.untapGroup(GroupFilter.AllCreaturesYouControl),
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        description = "When this enchantment enters, if it's your main phase, there is an " +
            "additional combat phase after this phase followed by an additional main phase. " +
            "When you next attack this turn, untap each creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "167"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b74876d8-f6a6-4b47-b960-b01a331bab01.jpg?1743204636"
    }
}
