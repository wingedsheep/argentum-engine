package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glorious End
 * {2}{R}
 * Instant
 *
 * End the turn.
 * At the beginning of your next end step, you lose the game.
 *
 * [Effects.EndTheTurn] is carried out once the spell has finished resolving (CR 724.1), so the
 * delayed trigger scheduled in the same resolution survives it, and Glorious End is exiled with
 * the stack. The trigger is pinned to your own end step (`fireOnPlayer = You`) with
 * [DelayedTriggerTiming.NEXT_END_STEP]: cast on your turn, that turn's end step is skipped by the
 * turn ending, so it fires at your following turn's end step; cast on an opponent's turn, it fires
 * at the end step of your very next turn. A one-shot step trigger: countered or otherwise removed
 * from the stack, it never triggers again (ruling).
 */
val GloriousEnd = card("Glorious End") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "End the turn. (Exile all spells and abilities from the stack, including this card. The player " +
        "whose turn it is discards down to their maximum hand size. Damage wears off, and \"this turn\" and " +
        "\"until end of turn\" effects end.)\n" +
        "At the beginning of your next end step, you lose the game."

    spell {
        effect = Effects.EndTheTurn then Effects.CreateDelayedTrigger(
            step = Step.END,
            timing = DelayedTriggerTiming.NEXT_END_STEP,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.LoseGame(),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2922b976-7beb-4c68-b39e-1b66d5c6f65e.jpg?1783936489"
        ruling(
            "2017-04-18",
            "Ending the turn this way means the following things happen in order: 1) All spells and abilities on " +
                "the stack are exiled. This includes spells and abilities that can't be countered. 2) If there are " +
                "any attacking and blocking creatures, they're removed from combat. 3) State-based actions are " +
                "checked. No player gets priority, and no triggered abilities are put onto the stack. 4) The " +
                "current phase and/or step ends. The game skips straight to the cleanup step. 5) The cleanup step " +
                "happens in its entirety."
        )
        ruling(
            "2017-04-18",
            "If any triggered abilities do trigger during this process, they're put onto the stack during the " +
                "cleanup step. If this happens, players will have a chance to cast spells and activate abilities, " +
                "then there will be another cleanup step before the turn is over."
        )
        ruling(
            "2017-04-18",
            "Though other spells and abilities that are exiled won't get a chance to resolve, they don't count as " +
                "being countered."
        )
        ruling(
            "2017-04-18",
            "Any \"at the beginning of the next end step\" triggered abilities won't get the chance to trigger that " +
                "turn because the end step is skipped. Those abilities will trigger at the beginning of the end step " +
                "of the next turn."
        )
        ruling(
            "2017-04-18",
            "If Glorious End's delayed triggered ability is countered, it won't trigger again. The same is true if " +
                "it's removed from the stack in any other way (such as by a second even more Glorious End) or if it " +
                "resolves and you don't lose the game (perhaps because of the emblem of Gideon of the Trials)."
        )
    }
}
