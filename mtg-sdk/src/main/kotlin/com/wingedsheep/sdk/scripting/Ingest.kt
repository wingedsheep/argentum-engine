package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ingest (CR 702.115) as pure data — the triggered ability every ingest creature has and none of
 * them prints as a separate line:
 *
 * > "Ingest" means "Whenever this creature deals combat damage to a player, that player exiles
 * > the top card of their library." (CR 702.115a)
 *
 * Pure composition over existing vocabulary, the same derivation shape as [Myriad]:
 * `Triggers.self.dealsCombatDamage(to = AnyPlayer)` → `Patterns.Library.exileTop(1,
 * PlayerRef(TriggeringPlayer))` — the damaged player is the trigger's player, so in multiplayer it
 * is the player this creature actually hit, and the card goes to that player's exile face up. An
 * empty library exiles nothing. Damage to a planeswalker or a creature doesn't trigger it.
 *
 * Instances: each one triggers separately (CR 702.115b). Printed instances are the
 * `KeywordAbility.Simple(INGEST)` entries on the card, or one for a bare `keywords(Keyword.INGEST)`;
 * lost with all abilities. A static grant floats the bare keyword, which projection can't count, so a
 * granted-only creature has exactly one instance.
 */
object Ingest {

    /** Ability id prefix; the instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "ingest"

    /** The number of ingest instances printed on [cardDef] — zero when it has none. */
    fun printedCount(cardDef: CardDefinition): Int {
        val abilities = cardDef.keywordAbilities.count {
            it is KeywordAbility.Simple && it.keyword == Keyword.INGEST
        }
        return if (abilities > 0) abilities else if (Keyword.INGEST in cardDef.keywords) 1 else 0
    }

    /** CR 702.115a — the ingest trigger for one instance. [instance] only distinguishes ids. */
    fun trigger(instance: Int = 0): TriggeredAbility {
        val spec = Triggers.self.dealsCombatDamage(to = Recipient.AnyPlayer)
        return TriggeredAbility(
            id = AbilityId(if (instance == 0) ABILITY_ID_PREFIX else "${ABILITY_ID_PREFIX}_$instance"),
            trigger = spec.event,
            binding = spec.binding,
            activeZones = setOf(Zone.BATTLEFIELD),
            effect = Patterns.Library.exileTop(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            descriptionOverride = "Ingest",
        )
    }
}
