package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Add Soulshift [n] (CR 702.46) — the display keyword plus the triggered ability it abbreviates:
 *
 * > **CR 702.46a** — "Soulshift N" means "When this permanent is put into a graveyard from the
 * > battlefield, you may return target Spirit card with mana value N or less from your graveyard to
 * > your hand."
 *
 * Pure composition: an optional [Triggers.self]`.dies()` trigger targeting a Spirit card you own in
 * your graveyard. The target is chosen as the trigger goes on the stack, when the dying permanent
 * is already in the graveyard — so a Spirit with a large enough N may return itself. "Spirit card"
 * is a subtype test on any card type (a Kindred Spirit qualifies). Each call adds a separate
 * triggered ability, so multiple instances trigger separately (CR 702.46b).
 */
fun CardBuilder.soulshift(n: Int) {
    require(n >= 0) { "Soulshift N needs a non-negative N, got $n" }
    keywordAbility(KeywordAbility.Numeric(Keyword.SOULSHIFT, n))
    triggeredAbility {
        trigger = Triggers.self.dies()
        optional = true
        val spirit = target(
            TargetFilter(
                baseFilter = GameObjectFilter.Any.withSubtype("Spirit").manaValueAtMost(n).ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.ReturnToHand(spirit)
        description = "Soulshift $n (When this creature dies, you may return target Spirit card with " +
            "mana value $n or less from your graveyard to your hand.)"
    }
}
