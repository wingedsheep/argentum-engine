package com.wingedsheep.ai.audit

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pins what the AI's static card readers make of a small fixed card list, so their coverage cannot
 * silently regress (and a fix shows up as a deliberate edit here). The full-corpus walk is
 * `just ai-reading-audit`; see `docs/ai/reading-audit.md`.
 *
 * When a reader learns an effect type, the card's expected flags shrink: update the entry in the
 * same change. When a reader loses one, this fails.
 */
class AiReadingAuditSnapshotTest : FunSpec({

    val expected: Map<String, List<String>> = mapOf(
        // Read fully — each one a shape some reader learned on purpose.
        "Lightning Bolt" to emptyList(),
        "Giant Growth" to emptyList(),
        "Pacifism" to emptyList(),
        "Icy Manipulator" to emptyList(),
        "Llanowar Elves" to emptyList(),
        "Smuggler's Copter" to emptyList(),
        // A +1/+1 counter on itself needs no intent tag (see AiReadingAudit.needsNoIntent).
        "Ajani's Pridemate" to emptyList(),
        // A +1/+1 counter on a target creature is a pump that never wears off.
        "Battlegrowth" to emptyList(),
        // Equip's target reads as what the Equipment does to the creature it equips; the attach
        // itself needs no intent tag.
        "+2 Mace" to emptyList(),
        // Shuffling itself into its library when it dies has no target and no window: no intent tag.
        "Alabaster Dragon" to emptyList(),
        // Mixed +4/-1: whether that helps depends on the creature, but equip only reaches our own
        // creatures, so its target is open for the simulation to pick the one that wears it best.
        "Barbed Battlegear" to emptyList(),
        // A pump + keyword trick is held whole by HoldPolicy's combat-trick branch: the keyword is timed.
        "Adamant Will" to emptyList(),
        // An end-of-turn damage shield on an activation waits for damage that can still come.
        "Abuna Acolyte" to emptyList(),
        // A prevention spell's shield is timed by simulation through the damage it stops.
        "Angelsong" to emptyList(),
        // A keyword-only end-of-turn grant spell is floored where no fight is left this turn.
        "Defy Gravity" to emptyList(),
        // "If it would die this turn, exile it instead" rides on removal: no window of its own.
        "Essence Burn" to emptyList(),
        // "Proliferate X times" reads as proliferate, which needs no intent tag.
        "Tam, the Possibility" to emptyList(),
        // A chosen-source silence on an activation: spent only on damage still to come this turn.
        "Burrenton Forge-Tender" to emptyList(),
        // Unread — the effect types the audit names as the work to do.
        // A draw beside the trample outlasts the turn, so the keyword floor does not read the spell.
        "Charge Through" to listOf("timing: GrantKeywordEffect"),
        "Abzan Charm" to emptyList(),
        "Act on Impulse" to listOf("intent: GrantMayPlayFromExileEffect", "intent: Pipeline(TopOfLibrary→exile)"),
        // "Target creature can't attack or block this turn": the can't-block reads as evasion for our
        // attack, which reads the ability's intent; combat and end-of-turn timing are still unread.
        "Alchemist's Vial" to listOf(
            "combat: CantBlockEffect", "timing: CantAttackEffect", "timing: CantBlockEffect",
        ),
        // "Target player reveals their hand and discards all nonland cards": read as discard, aimed at
        // the other side's hand.
        "Amnesia" to emptyList(),
    )

    test("the AI's readers flag exactly the pinned effect types on each snapshot card") {
        val byName = MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name }
        val actual = expected.keys.associateWith { name ->
            val card = byName[name] ?: error("snapshot card '$name' is not in the corpus")
            AiReadingAudit.audit(card).map { it.key }
        }
        actual shouldBe expected
    }
})
