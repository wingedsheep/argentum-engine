package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Belltower Sphinx — Ravnica: City of Guilds #38
 * {4}{U} · Creature — Sphinx · 2/5
 *
 * Flying
 * Whenever a source deals damage to this creature, that source's controller mills that many cards.
 *
 * A 2/5 flying wall that taxes every answer: block it, burn it, or fight it, and you mill yourself
 * for the trouble. Both halves are ordinary vocabulary, but the join between them was the gap —
 * "**that source's** controller" needs the *damage source*. "A source deals damage to this" is the
 * per-source form, `Triggers.self.isDealtDamage(by = GameObjectFilter.Any)`, which binds the damage
 * source as the triggering entity (as Tephraderm's `Creature` filter does), so
 * `Player.ControllerOfTriggeringEntity` names the right player here. The bare
 * `Triggers.self.isDealtDamage()` is "is dealt damage", which binds the damaged creature.
 *
 * **"That many" is per damage event, not per turn.** The engine emits one `DamageDealtEvent` per
 * source/recipient pair, so being blocked by two 2/2s is two separate 2-damage instances and mills
 * each blocker's controller 2 — not one controller 4. That is how the printed card behaves.
 *
 * **Lethal damage still mills.** State-based actions bury the Sphinx before the ability resolves,
 * but the trigger was already detected off the damage event (CR 603.10), and
 * `DamageTriggerDetector.detectDamagedBySourceTriggers` deliberately looks the creature up after it
 * has left the battlefield for exactly this case.
 *
 * **A burn spell mills its caster even though it is gone by then.** The damage source for a
 * Lightning Bolt finishes resolving and leaves the stack before this trigger does, so
 * `Player.ControllerOfTriggeringEntity` falls through `TargetResolutionUtils.controllerOf`'s ladder
 * to the spell's last-known controller and finally its owner (CR 608.2h).
 *
 * Canonical printing note: Scryfall lists a `psal` (Salvat 2005) printing seven weeks before RAV,
 * but that is a regional box product rather than a real expansion, so the canonical definition
 * stays here. `SelesnyaSanctuary` and `SeedSpark` document the same call for this set.
 */
val BelltowerSphinx = card("Belltower Sphinx") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 2
    toughness = 5
    oracleText = "Flying\nWhenever a source deals damage to this creature, that source's " +
        "controller mills that many cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage(by = GameObjectFilter.Any)
        effect = Patterns.Library.mill(
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.ControllerOfTriggeringEntity,
        )
        description = "Whenever a source deals damage to this creature, that source's controller " +
            "mills that many cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/452a23a0-62de-4561-b361-9c0de9151129.jpg?1783943691"
    }
}
