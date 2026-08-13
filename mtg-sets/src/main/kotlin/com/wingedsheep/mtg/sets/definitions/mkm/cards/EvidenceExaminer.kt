package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Evidence Examiner — Murders at Karlov Manor #201
 * {G}{U} · Creature — Merfolk Detective · 2/2 · Uncommon
 *
 * At the beginning of combat on your turn, you may collect evidence 4.
 * Whenever you collect evidence, investigate.
 *
 * A self-contained engine: the first ability feeds the second, turning a stocked graveyard into a
 * Clue every turn. Two mana for a 2/2 that converts dead cards into live ones is a fine rate in a
 * grindy limited deck, and the two halves are deliberately *unlinked* — the payoff fires for
 * evidence collected in **any** context, so an activated-ability cost, a cast cost, or an
 * Axebane Ferox-style ward cost paid elsewhere all also produce a Clue.
 *
 * The same shape as [SurveillanceMonitor], with the self-feeding trigger moved from enters to
 * beginning of combat. Both halves come straight off the existing rails:
 *
 * - [Triggers.BeginCombat] is already scoped to your turn (`Step.BEGIN_COMBAT, Player.You`), so the
 *   "on your turn" clause needs no extra condition.
 * - The collect is a bare "you may" with no rider, so it is [Effects.CollectEvidence] under a
 *   [MayEffect] gate rather than a reflexive trigger — there is no "when you do" to put on the
 *   stack, and the Clue arrives via the separate payoff trigger instead. Per CR 701.59b the prompt
 *   is skipped entirely when the graveyard can't reach total mana value 4, so a player is never
 *   offered a collection they couldn't complete.
 * - [Triggers.WheneverYouCollectEvidence] fires once per collection, after the cards are exiled —
 *   never for a declined collect, nor for one CR 701.59b made impossible. That is what makes the
 *   two abilities chain without any explicit linkage between them.
 *
 * Collecting evidence 4 is a *sum*, not a count, so a single exiled 4-drop suffices while four
 * lands never do; over-paying (exiling a 6-drop) is legal.
 */
val EvidenceExaminer = card("Evidence Examiner") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Merfolk Detective"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, you may collect evidence 4. (Exile " +
        "cards with total mana value 4 or greater from your graveyard.)\n" +
        "Whenever you collect evidence, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = MayEffect(Effects.CollectEvidence(4))
        description = "At the beginning of combat on your turn, you may collect evidence 4."
    }

    triggeredAbility {
        trigger = Triggers.WheneverYouCollectEvidence
        effect = Effects.Investigate()
        description = "Whenever you collect evidence, investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Paolo Puggioni"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f53a6ee7-86e1-4d2d-994c-214e0ec08dad.jpg?1783912852"

        ruling(
            "2024-02-02",
            "Evidence Examiner's last ability triggers whenever you collect evidence for any " +
                "reason, not just when you collect evidence with its first ability."
        )
        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you can't " +
                "choose to collect evidence at all."
        )
    }
}
