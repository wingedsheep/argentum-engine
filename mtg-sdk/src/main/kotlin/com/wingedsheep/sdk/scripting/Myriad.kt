package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Myriad (CR 702.116) as pure data — the triggered ability every myriad creature has and none of
 * them prints as a separate line:
 *
 * > "Myriad" means "Whenever this creature attacks, for each opponent other than defending player,
 * > you may create a token that's a copy of this creature that's tapped and attacking that player
 * > or a planeswalker they control. If one or more tokens are created this way, exile the tokens
 * > at end of combat." (CR 702.116a)
 *
 * Pure composition over existing vocabulary:
 *
 * - **"for each opponent other than defending player"** — `ForEachPlayer(EachOpponent)` gated per
 *   visit by `Not(PlayerIs(You, DefendingPlayer))`; inside the loop `Player.You` is the visited
 *   opponent, while `DefendingPlayer` still reads the attacking source's own defender. In a
 *   two-player game the only opponent is the defending player, so myriad does nothing.
 * - **"you may create"** — `May`, asked of the source's controller rather than the visited
 *   opponent (the loop rebinds "you"), and the copy is created under that same controller.
 * - **"a copy of this creature that's tapped and attacking that player"** —
 *   `CreateTokenCopyOfTarget(Self, tapped, attacking, attackedPlayer = You)`. The copy *enters*
 *   attacking, so it never "attacked" (CR 508.4) and its own myriad doesn't trigger. The engine
 *   picks the player rather than one of their planeswalkers, the same CR 508.4 simplification
 *   every "enters attacking" effect here makes.
 * - **"exile the tokens at end of combat"** — `exileAtStep = END_COMBAT`, one delayed exile per
 *   copy.
 *
 * Instances: each one triggers separately (CR 702.116b). Printed instances are the
 * `KeywordAbility.Simple(MYRIAD)` entries on the card ("Myriad, myriad" is two), or one for a bare
 * `keywords(Keyword.MYRIAD)`; lost with all abilities. A static grant floats the bare keyword,
 * which projection can't count, so a granted-only creature has exactly one instance.
 */
object Myriad {

    /** Ability id prefix; the instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "myriad"

    /** The number of myriad instances printed on [cardDef] — zero when it has none. */
    fun printedCount(cardDef: CardDefinition): Int {
        val abilities = cardDef.keywordAbilities.count {
            it is KeywordAbility.Simple && it.keyword == Keyword.MYRIAD
        }
        return if (abilities > 0) abilities else if (Keyword.MYRIAD in cardDef.keywords) 1 else 0
    }

    /** CR 702.116a — the myriad trigger for one instance. [instance] only distinguishes ids. */
    fun trigger(instance: Int = 0): TriggeredAbility {
        val spec = Triggers.self.attacks()
        val sourceController = EffectTarget.PlayerRef(Player.ControllerOfSource)
        return TriggeredAbility(
            id = AbilityId(if (instance == 0) ABILITY_ID_PREFIX else "${ABILITY_ID_PREFIX}_$instance"),
            trigger = spec.event,
            binding = spec.binding,
            activeZones = setOf(Zone.BATTLEFIELD),
            effect = Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.If(
                    Conditions.Not(Conditions.PlayerIs(Player.You, Player.DefendingPlayer)),
                    Effects.May(
                        Effects.CreateTokenCopyOfTarget(
                            target = EffectTarget.Self,
                            tapped = true,
                            attacking = true,
                            attackedPlayer = EffectTarget.PlayerRef(Player.You),
                            controller = sourceController,
                            exileAtStep = Step.END_COMBAT,
                        ),
                        decisionMaker = sourceController,
                        prompt = "Create a token copy attacking this opponent?",
                    ),
                ),
            ),
            descriptionOverride = "Myriad",
        )
    }
}
