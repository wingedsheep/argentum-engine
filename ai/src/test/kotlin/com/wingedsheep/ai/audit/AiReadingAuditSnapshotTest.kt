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
        // Unread — the effect types the audit names as the work to do.
        "Adamant Will" to listOf("timing: GrantKeywordEffect"),
        "Abuna Acolyte" to listOf("timing: PreventDamageEffect"),
        "+2 Mace" to listOf("intent: AttachEquipmentEffect", "polarity: AttachEquipmentEffect"),
        "Abzan Charm" to listOf("polarity: DistributeCountersAmongTargetsEffect"),
        "Act on Impulse" to listOf("intent: GrantMayPlayFromExileEffect", "intent: Pipeline(TopOfLibrary→exile)"),
        "Alchemist's Vial" to listOf(
            "combat: CantBlockEffect", "intent: CantAttackEffect", "intent: CantBlockEffect",
            "timing: CantAttackEffect", "timing: CantBlockEffect",
        ),
        "Amnesia" to listOf(
            "intent: Pipeline(hand→graveyard)", "intent: RevealHandEffect",
            "polarity: Pipeline(hand→graveyard)", "polarity: RevealHandEffect",
        ),
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
