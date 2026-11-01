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
        // Mixed +4/-1: whether that helps depends on the creature, so equip's target stays unread.
        "Barbed Battlegear" to listOf("polarity: AttachEquipmentEffect"),
        // Unread — the effect types the audit names as the work to do.
        "Adamant Will" to listOf("timing: GrantKeywordEffect"),
        "Abuna Acolyte" to listOf("timing: PreventDamageEffect"),
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
